package my.cute.bot.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import my.cute.bot.util.ThoughtRetriever;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class ThoughtScrapeTask implements Runnable {
	private static class Thought {
		private String text;

		public Thought(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return "text=[" + text + "]";
		}
	}
	private static final Logger logger = LoggerFactory.getLogger(ThoughtScrapeTask.class);
	private static final int MESSAGE_WINDOW = 10;
	private static final int SELF_MESSAGE_WINDOW = 60;
	private static final int NUM_MESSAGES = 1000;
	//shouldn't hold references to jda entities, but this task should be shortlived
	private final MessageChannel channel;

	public ThoughtScrapeTask(MessageChannel channel) {
		this.channel = channel;
	}

	@Override
	public void run() {
		List<Thought> buff = Collections.synchronizedList(new ArrayList<>());
		Set<String> checked = Collections.synchronizedSet(new HashSet<>());
		List<Message> messages = Collections.synchronizedList(new ArrayList<>());
		List<Message> curMessages;
		long skipID = 0;
		try {
			do {
				curMessages = retrieveMessages(channel, NUM_MESSAGES, skipID).get();
				messages.addAll(curMessages);
				skipID = curMessages.getLast().getIdLong();
			} while(curMessages.size() == NUM_MESSAGES);
			//			curMessages = retrieveMessages(channel, numMessages, skipID).thenApply(messages -> {
			for(int i = 0; i < messages.size(); i++) {
				int curIndex = i;
				Message msg = messages.get(i);
				if(msg.getAuthor().isBot()) continue;
				if(checked.contains(msg.getId())) {
					System.out.println("skipping already checked msg");
					continue;
				}
				System.out.println("processing new thought");
				String thought = searchMessages(messages, curIndex, checked);
				if(thought.isBlank()) continue;
				buff.add(new Thought(thought));
				if(buff.size() % 50 == 0) {
					synchronized(this) {
						writeOutput(buff, "./text.json");
						writeLastMessageID(msg.getId());
						buff.clear();
					}
				}
			}
			synchronized(this) {
				writeOutput(buff, "./text.json");
				buff.clear();
			}
			System.out.println("finished processing message list");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	private String searchMessages(List<Message> messages, int start, Set<String> checked) {
		List<String> messageContents = new LinkedList<>();
		int curIndex = start;
		Message msg = messages.get(curIndex);
		Message startMessage = msg;
		messageContents.add(msg.getContentDisplay());
		checked.add(msg.getId());
		System.out.println("Starting message: " + startMessage.getContentDisplay());
		/*
		 * we stop when we hit a reply.
		 * if the initial message was a reply, it must have had no further thoughts in front of it, or we would have
		 * started there instead.
		 * so it's a lone thought as a reply.
		 * ie, only check further messages if initial message is not a reply
		 */
		if(startMessage.getType() != MessageType.INLINE_REPLY) {
			curIndex++;
			OffsetDateTime startTime = msg.getTimeCreated();
			OffsetDateTime lastMessageTime = startTime;
			while(curIndex < messages.size()) {
				Message curMessage = messages.get(curIndex);
				//does this happen?
				if(curMessage.getAuthor() == null) continue;

				if(curMessage.getAuthor().equals(startMessage.getAuthor())) {
					if(curMessage.getType() == MessageType.INLINE_REPLY) {
						//replying to another message is usually an indicator of a new thought
						System.out.println("hit a reply, ending search");
						break;
					}
					if(curMessage.getTimeCreated().until(lastMessageTime, ChronoUnit.SECONDS) > SELF_MESSAGE_WINDOW) {
						//enforce time limit between same user messages
						System.out.println("self message window limit reached, ending search");
						break;
					}
					//note our messageContents list is ordered opposite from jda lists; jda is new->old, messageContents is old->new
					messageContents.addFirst(curMessage.getContentDisplay());
					checked.add(curMessage.getId());
					lastMessageTime = curMessage.getTimeCreated();
				} else if(curMessage.getTimeCreated().until(lastMessageTime, ChronoUnit.SECONDS) > MESSAGE_WINDOW) {
					System.out.println("message window limit reached, ending search");
					break;
				}
				curIndex++;
			}
		}

		String result = ThoughtRetriever.joinThoughtFragments(messageContents);
		System.out.printf("final msg: [%s]%n", result);
		return result;
	}

	private void writeLastMessageID(String messageID) {
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of("./lastmsg-text.txt"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writer.append(messageID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeOutput(List<Thought> thoughts, String path) {
		System.out.println("writing " + thoughts.size() + " thoughts");
		Gson gson = new Gson();
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of(path), StandardCharsets.UTF_8, StandardOpenOption.CREATE, 
				StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			for(Thought thought : thoughts) {
				StringBuilder sb = new StringBuilder();
				sb.append(gson.toJson(thought));
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
