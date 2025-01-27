package my.cute.bot.util;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.requests.RestAction;

public class ThoughtRetriever {
	private static final Pattern INSERT_PERIOD_PATTERN = Pattern.compile(".+(?:\\w|\\\"|\\(|\\))$");
	//in seconds
	private static final int MESSAGE_WINDOW = 10;
	private static final int SELF_MESSAGE_WINDOW = 60;
	private static final int NUM_SEARCHED_MESSAGES = 20;

	public static String buildThoughtAround(List<Message> messages, int start) {
		Message startMessage = messages.get(start);
		List<String> messageContents = new LinkedList<>();
		System.out.println("Starting message: " + startMessage.getContentDisplay());
		messageContents.add(startMessage.getContentDisplay());
		System.out.println("checking newer msgs");
		/*
		 * build longer messages from multi-message thoughts. 
		 * many people just send sentences as separate messages rather than use periods,
		 * so from our starting message, move to the next message. if it's from the same user,
		 * add it as a continuation of the original message. continue until a message from another user is found.
		 * then, proceed until a message is found that's at least <time window> seconds after the original author's 
		 * last message. this indicates that they've finished their thought. if another message from the user is found
		 * within that time window, continue their thought.
		 * possible that there's a significant gap between messages by the same user representing a separation in thoughts,
		 * and just no one else messaged in the meantime, but this should be uncommon enough that we can ignore it
		 * do this in both directions (new/old) to get the complete thought.
		 */
		//messages is ordered from new to old. check newer messages first
		OffsetDateTime lastMessageTime = startMessage.getTimeCreated();
		for(int i = start - 1; i >= 0; i--) {
			Message curMessage = messages.get(i);
			//does this happen?
			if(curMessage.getAuthor() == null) continue;

			if(curMessage.getAuthor().equals(startMessage.getAuthor())) {
				if(curMessage.getType() == MessageType.INLINE_REPLY) {
					//replying to another message is usually an indicator of a new thought
					break;
				}
				if(lastMessageTime.until(curMessage.getTimeCreated(), ChronoUnit.SECONDS) > SELF_MESSAGE_WINDOW) {
					//enforce time limit between same user messages
					break;
				}
				//note our messageContents list is ordered opposite from jda lists; jda is new->old, messageContents is old->new
				messageContents.add(curMessage.getContentDisplay());
				lastMessageTime = curMessage.getTimeCreated();
			} else if(lastMessageTime.until(curMessage.getTimeCreated(), ChronoUnit.SECONDS) > MESSAGE_WINDOW) {
				break;
			}
		}
		System.out.println("checking older msgs");
		for(int i = start + 1; i < messages.size(); i++) {
			Message curMessage = messages.get(i);
			//does this happen?
			if(curMessage.getAuthor() == null) continue;

			if(curMessage.getAuthor().equals(startMessage.getAuthor())) {
				if(curMessage.getType() == MessageType.INLINE_REPLY) {
					//replying to another message is usually an indicator of a new thought
					break;
				}
				if(curMessage.getTimeCreated().until(lastMessageTime, ChronoUnit.SECONDS) > SELF_MESSAGE_WINDOW) {
					//enforce time limit between same user messages
					break;
				}
				//note our messageContents list is ordered opposite from jda lists; jda is new->old, messageContents is old->new
				messageContents.addFirst(curMessage.getContentDisplay());
				lastMessageTime = curMessage.getTimeCreated();
			} else if(curMessage.getTimeCreated().until(lastMessageTime, ChronoUnit.SECONDS) > MESSAGE_WINDOW) {
				break;
			}
		}
		String result = joinThoughtFragments(messageContents);
		System.out.printf("final msg: [%s]%n", result);
		return result;
	}

	public static String joinThoughtFragments(List<String> messageContents) {
		StringBuilder sb = new StringBuilder();
		sb.append(messageContents.get(0));
		for(int i = 1; i < messageContents.size(); i++) {
			if(INSERT_PERIOD_PATTERN.matcher(messageContents.get(i-1)).matches()) {
				sb.append(".");
			}
			sb.append(" ");
			sb.append(messageContents.get(i));
		}

		String result = sb.toString();
		return result;
	}

	public static RestAction<String> buildThoughtAround(Message startMessage) {
		System.out.println("starting getHistoryAround action");
		return startMessage.getChannel().getHistoryAround(startMessage, NUM_SEARCHED_MESSAGES).map(history -> {
			List<Message> messages = history.getRetrievedHistory();
			int startIndex = -1;
			for(int i=0; i < messages.size(); i++) {
				if(messages.get(i).equals(startMessage)) {
					System.out.println("found matching msg: " + startMessage.getContentDisplay());
					startIndex = i;
					break;
				}
			}
			if(startIndex == -1) throw new AssertionError("couldn't find message w/ id " + startMessage.getId() + "?");

			return buildThoughtAround(messages, startIndex);
		});
	}
}
