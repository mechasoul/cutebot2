package my.cute.bot.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import my.cute.bot.util.ThoughtRetriever;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;

public class ConversationScrapeTask implements Runnable {
	private static class Conversation {
		private String instruction;
		private String output;

		public Conversation(String instruction, String output) {
			this.instruction = instruction;
			this.output = output;
		}

		@Override
		public String toString() {
			return "instruction=[" + instruction + "], output=[" + output + "]";
		}
	}
	private static final Logger logger = LoggerFactory.getLogger(ConversationScrapeTask.class);
	private static final int NUM_MESSAGES = 1000;
	//shouldn't hold references to jda entities, but this task should be shortlived
	private final MessageChannel channel;

	public ConversationScrapeTask(MessageChannel channel) {
		this.channel = channel;
	}

	@Override
	public void run() {
		List<Conversation> buff = Collections.synchronizedList(new ArrayList<>());
		List<CompletableFuture<Void>> actions = Collections.synchronizedList(new ArrayList<>());
		long skipID = 851929696455360522L;
		//		long skipID = 0;
		int numMessages = NUM_MESSAGES;
		try {
			List<Message> curMessages;
			do {
				curMessages = retrieveMessages(channel, numMessages, skipID).thenApply(messages -> {
					for(int i = messages.size() - 1; i >= 0; i--) {
						int currentIndex = i;
						Message msg = messages.get(i);
						if(msg.getType() == MessageType.INLINE_REPLY && !msg.getAuthor().isBot()) {
							System.out.println("found reply: " + msg.getContentDisplay());
							MessageReference reference = msg.getMessageReference();
							if(reference != null) {
								RestAction<Message> resolveAction;
								try {
									resolveAction = reference.resolve();
								} catch (IllegalStateException e) {
									//triggered from referenced message not having a channel for some reason. 
									//happens on every inline_reply after a specific point...?
									resolveAction = null;
								}
								CompletableFuture<Message> messageFuture;
								if(resolveAction != null) {
									//normal handling
									System.out.println("adding new conversation action");
									messageFuture = resolveAction.submit().handle((message, throwable) -> {
										if(throwable != null) {
											if(throwable instanceof ErrorResponseException) {
												ErrorResponseException e = (ErrorResponseException)throwable;
												if(e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE) {
													System.err.printf("found unknown message, continuing%n");
												} else {
													logger.warn("unknown error response?", e);
													e.printStackTrace();
												}
											} else {
												logger.warn("unknown exception?", throwable);
												throwable.printStackTrace();
											}
											return null;
										} else {
											return message;
										}
									});
								} else {
									//can't resolve referenced message. try to use referenced message directly
									//apply a delay so jda has time to fetch the message on its own
									System.out.println("attempting to retrieve referenced message directly in 5s");
									messageFuture = CompletableFuture.supplyAsync(() -> msg.getReferencedMessage(), 
											CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
								}
								actions.add(messageFuture.thenComposeAsync(originalMsg -> {
										if(originalMsg != null && !originalMsg.getAuthor().isBot() && !originalMsg.getAuthor().equals(msg.getAuthor())) {
											String builtMsg = ThoughtRetriever.buildThoughtAround(messages, currentIndex);
											return ThoughtRetriever.buildThoughtAround(originalMsg).submit().thenAcceptAsync(builtOriginalMsg -> {
												buff.add(new Conversation(builtOriginalMsg, builtMsg));
												if(buff.size() % 50 == 0) {
													synchronized(this) {
														writeOutput(buff, "./out.json");
														writeLastMessageID(msg.getId());
														buff.clear();
													}
												}
											});
										} else {
											return CompletableFuture.completedFuture(null);
										}
									}));
								if(actions.size() % 50 == 0) System.out.println(actions.size());
							}
						}
					}
					System.out.println("finished processing current message list");
					return messages;
				}).get();
				skipID = curMessages.getLast().getIdLong();
			} while(curMessages.size() == numMessages);
			CompletableFuture.allOf(actions.stream().toArray(CompletableFuture[]::new)).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private void writeLastMessageID(String messageID) {
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of("./lastmsg.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(messageID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutput(List<Conversation> conversations, String path) {
		System.out.println("writing " + conversations.size() + " conversations");
		Gson gson = new Gson();
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			for(Conversation convo : conversations) {
				StringBuilder sb = new StringBuilder();
				sb.append(gson.toJson(convo));
				writer.write(sb.toString());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		};
	}

	private CompletableFuture<List<Message>> retrieveMessages(MessageChannel channel, int numMessages, long skipID) {
		return channel.getIterableHistory().cache(false).skipTo(skipID).takeAsync(numMessages);
	}
}
