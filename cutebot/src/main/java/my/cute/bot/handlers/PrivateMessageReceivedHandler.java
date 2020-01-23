package my.cute.bot.handlers;

import my.cute.bot.MyListener;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class PrivateMessageReceivedHandler {

	private final MyListener bot;
	
	public PrivateMessageReceivedHandler(MyListener bot) {
		this.bot = bot;
	}
	
	public void handle(PrivateMessageReceivedEvent event) {
		if (event.getAuthor().getId().equals("115618938510901249") && event.getMessage().getContentDisplay().equals("!exit")) {
			event.getChannel().sendMessage("ok").queue(msg -> this.bot.shutdown(), ex -> this.bot.shutdown());
		} else {
			event.getChannel().sendMessage("??").queue();
		}
	}
}
