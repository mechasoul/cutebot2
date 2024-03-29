package my.cute.bot.commands;

import my.cute.bot.MyListener;
import my.cute.bot.database.GuildDatabase;
import net.dv8tion.jda.api.entities.Message;

public class PrivateChannelExportCommand extends PrivateChannelCommand {

	final static String NAME = "export";
	private final static String DESCRIPTION = "exports the provided guild's database to a txt file";
	
	private final MyListener bot;
	
	public PrivateChannelExportCommand(MyListener bot) {
		super(NAME, DESCRIPTION, PermissionLevel.DEVELOPER, 1, 1);
		this.bot = bot;
	}
	
	@Override
	public void execute(Message message, String[] params) {
		GuildDatabase db = this.bot.getDatabase(params[1]);
		if(db != null) {
			message.getChannel().sendMessage("exporting database to txt for guild id '" + params[1] + "'").queue();
			db.exportToText();
		} else {
			message.getChannel().sendMessage("no such guild id found").queue();
		}
	}

}
