package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.database.GuildDatabase;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelExportCommand extends PrivateChannelCommand {

	private final MyListener bot;
	
	public PrivateChannelExportCommand(MyListener bot) {
		super("export", PermissionLevel.DEVELOPER, 1, 1, false);
		this.bot = bot;
	}
	
	@Override
	public void execute(Message message) {
		String words[] = message.getContentDisplay().split("\\s");
		GuildDatabase db = this.bot.getDatabase(words[1]);
		if(db != null) {
			message.getChannel().sendMessage("exporting database to txt for guild id '" + words[1] + "'").queue();
			db.exportToText();
		} else {
			message.getChannel().sendMessage("no such guild id found").queue();
		}
	}

}
