package my.cute.bot.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RegexValidator {

	//in ms
	private static final long TIMEOUT_THRESHOLD = 50;
	private static final long NUM_LINES = 400000;
	private static final long ENTIRE_PROCESS_TIMEOUT = 300000;
	
	/**
	 * tests a given Pattern against a collection of arbitrary input similar to
	 * what the Pattern is likely going to be checked against. runs the same
	 * pattern-finding code that will be used by the bot, and if any attempted
	 * match exceeds the timeout threshold (as specified by TIMEOUT_THRESHOLD,
	 * in ms), returns false.
	 * <p>
	 * note that for this test, the threshold for a timeout is considerably
	 * lower than the actual threshold for a timeout during bot operation. i
	 * think this makes sense because this is a random collection of some lines
	 * that are the sort of random lines the bot will encounter during operation,
	 * but the actual breadth of what it might run into could be a lot wider than
	 * this test. also this test runs on NUM_LINES lines of text, so in production
	 * environment lines are being processed at a much slower rate so we have
	 * leeway on time taken to check the pattern 
	 * <p>
	 * note also that the entire method has a timeout threshold, since at maximum 
	 * the whole test could take about (TIMEOUT_THRESHOLD * NUM_LINES) ms, which 
	 * could be a pretty long time. it's extremely unlikely that a regex would
	 * consistently take that much time without ever going over the threshold
	 * (much more likely it'd be extremely fast for a vast majority of inputs 
	 * and unacceptably slow for certain specific inputs), but i guess it's 
	 * possible, so there's a (still extremely forgiving) max duration for the
	 * whole process
	 * @param pattern The pattern to test for timeout with
	 * @return true if the pattern passed all tests, false if at least one test
	 * was failed
	 * @throws IOException 
	 */
	public static boolean regexTimeoutTest(Pattern pattern) throws IOException {
		
		/*
		 * TODO this should probably all be moved to a future. fairly long process so probably
		 * don't want to block everything while this is happening...
		 */
		try {
			//future for specifying a timeout for entire process
			//note we need to get the IOExceptions out of lambdas because of this so it's really ugly. what a shame
			CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
				//warmup
				try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(1000)) {
					lines.forEach(line -> {
						try {
							MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							//ignore timeouts during warmup and continue
						} catch (InterruptedException | ExecutionException e) {
							throw new RuntimeException(e);
						}
					});
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				
				//test on many messages
				try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(NUM_LINES)) {
					lines.forEach(line -> {
						try {
							MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							throw new UncheckedTimeoutException(e);
						} catch (InterruptedException | ExecutionException e) {
							throw new RuntimeException(e);
						} 
					});
				} catch (UncheckedTimeoutException e) {
					//a test failed
					return false;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				
				//now test on long message
				try (BufferedReader reader = Files.newBufferedReader(Paths.get("./testlongmessage.txt"), StandardCharsets.UTF_8)) {
					MiscUtils.findMatchWithTimeout(pattern, reader.readLine(), TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					return false;
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				} 
				//all tests passed
				return true;
			});
			
			return future.get(ENTIRE_PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
			
		} catch (ExecutionException e) {
			//IOException wrapped by CompletableFuture. extract it
			if(e.getCause() instanceof UncheckedIOException) {
				throw ((UncheckedIOException)e.getCause()).getCause();
			} else if(e.getCause() instanceof RuntimeException) {
				//some other problem?
				throw ((RuntimeException)e.getCause());
			} else {
				//shouldnt be possible to have any other checked exceptions
				throw new AssertionError(e);
			}
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		} catch (TimeoutException e) {
			//whole validation process took too long
			return false;
		}
	}
	
	/**
	 * tests a given Pattern against a collection of arbitrary input similar to
	 * what the Pattern is likely going to be checked against. runs the same
	 * pattern-finding code that will be used by the bot, and if any attempted
	 * match exceeds the timeout threshold (as specified by TIMEOUT_THRESHOLD,
	 * in ms), returns false.
	 * <p>
	 * note that for this test, the threshold for a timeout is considerably
	 * lower than the actual threshold for a timeout during bot operation. i
	 * think this makes sense because this is a random collection of some lines
	 * that are the sort of random lines the bot will encounter during operation,
	 * but the actual breadth of what it might run into could be a lot wider than
	 * this test. also this test runs on NUM_LINES lines of text, so in production
	 * environment lines are being processed at a much slower rate so we have
	 * leeway on time taken to check the pattern 
	 * <p>
	 * note also that the entire method has a timeout threshold, since at maximum 
	 * the whole test could take about (TIMEOUT_THRESHOLD * NUM_LINES) ms, which 
	 * could be a pretty long time. it's extremely unlikely that a regex would
	 * consistently take that much time without ever going over the threshold
	 * (much more likely it'd be extremely fast for a vast majority of inputs 
	 * and unacceptably slow for certain specific inputs), but i guess it's 
	 * possible, so there's a (still extremely forgiving) max duration for the
	 * whole process
	 * <p>
	 * instead of executing tests on the calling thread like non-async version,
	 * this method executes tests via a task submitted to default forkjoinpool through
	 * CompletableFuture, and instead of returning a result directly, the result
	 * of the tests can be obtained through the returned CompletableFuture. note 
	 * that the returned CompletableFuture may complete exceptionally with a number
	 * of different exceptions
	 * @param pattern The pattern to test for timeout with
	 * @return a CompletableFuture that completes normally with true if all tests 
	 * passed successfully, or completes normally with false if a test failed via 
	 * timeout. it may also complete exceptionally with an IOException wrapped as
	 * UncheckedIOException (if IOException is encountered when
	 * opening test files to read), or with an InterruptedException or ExecutionException
	 * wrapped as RuntimeException (ExecutionException could occur if the provided 
	 * Pattern is invalid, for example)
	 */
	public static CompletableFuture<Boolean> regexTimeoutTestAsync(Pattern pattern) {
		return CompletableFuture.supplyAsync(() -> {
			try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(1000)) {
				lines.forEach(line -> {
					try {
						MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						//ignore timeouts during warmup and continue
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			
			try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(NUM_LINES)) {
				lines.forEach(line -> {
					try {
						MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
					} catch (TimeoutException e) {
						throw new UncheckedTimeoutException(e);
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					} 
				});
			} catch (UncheckedTimeoutException e) {
				return false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			
			try (BufferedReader reader = Files.newBufferedReader(Paths.get("./testlongmessage.txt"), StandardCharsets.UTF_8)) {
				MiscUtils.findMatchWithTimeout(pattern, reader.readLine(), TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				return false;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
			
			return true;
		});
	}
	
	public static String regexTimeoutTestWithResult(Pattern pattern) throws IOException {
		
		try {
			//future for specifying a timeout for entire process
			//note we need to get the IOExceptions out of lambdas because of this so it's really ugly. what a shame
			CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
				//warmup
				try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(1000)) {
					lines.forEach(line -> {
						try {
							MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
						} catch (TimeoutException e) {
							//ignore timeouts during warmup and continue
						} catch (InterruptedException | ExecutionException e) {
							throw new RuntimeException(e);
						}
					});
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				
				//test on many messages
				try (Stream<String> lines = Files.lines(Paths.get("./testinput.txt"), StandardCharsets.UTF_8).limit(NUM_LINES)) {
					lines.forEach(line -> {
							try {
								MiscUtils.findMatchWithTimeout(pattern, line, TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
							} catch (TimeoutException e) {
								throw new UncheckedTimeoutException(e);
							} catch (InterruptedException | ExecutionException e) {
								throw new RuntimeException(e);
							}
						});
				} catch (UncheckedTimeoutException e) {
					return e.getCause().getMessage();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				
				//now test on long message
				try (BufferedReader reader = Files.newBufferedReader(Paths.get("./testlongmessage.txt"), StandardCharsets.UTF_8)) {
					MiscUtils.findMatchWithTimeout(pattern, reader.readLine(), TIMEOUT_THRESHOLD, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					return "[longline]";
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				} 
				//all tests passed
				return null;
			});
			
			return future.get(5, TimeUnit.MINUTES);
			
		} catch (ExecutionException e) {
			//IOException wrapped by CompletableFuture. extract it
			if(e.getCause() instanceof UncheckedIOException) {
				throw ((UncheckedIOException)e.getCause()).getCause();
			} else if(e.getCause() instanceof RuntimeException) {
				//some other problem?
				throw ((RuntimeException)e.getCause());
			} else {
				//shouldnt be possible to have any other checked exceptions
				throw new AssertionError(e);
			}
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		} catch (TimeoutException e) {
			//whole validation process took too long
			return "[wholeprocess]";
		}
	}
	
	private static interface ConsumerWithTimeout<T> extends Consumer<T> {
		
		@Override
		default void accept(T t) {
			try {
				consumeWithTimeout(t);
			} catch (TimeoutException e) {
				if(t instanceof String) {
					throw new UncheckedTimeoutException(new TimeoutException((String)t));
				} else {
					throw new UncheckedTimeoutException(e);
				}
			}
		}
		
		void consumeWithTimeout(T t) throws TimeoutException;
	}
	
	private static class UncheckedTimeoutException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final TimeoutException e;
		
		private UncheckedTimeoutException(TimeoutException e) {
			this.e = e;
		}
		
		public TimeoutException getCause() {
			return this.e;
		}
	}
}
