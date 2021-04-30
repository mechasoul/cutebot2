package my.cute.bot.commands;

import net.dv8tion.jda.api.JDA;

public abstract class TextChannelCommand extends CommandImpl {
	
	/*
	 * since TextChannelCommands are associated with a specific guild, they include these
	 * parameters so that the guild instance can be accessed if necessary. note that jda
	 * recommends not storing a Guild object instance for long, so we store these instead
	 * 
	 * TODO i think i can delete this since any command execution contains a message which
	 * provides the guild instance, which then also means this entire class doesnt need to
	 * exist?
	 */
	protected final JDA jda;
	protected final String guildId;
	
	TextChannelCommand(String name, String description, PermissionLevel permission, int min, int max, JDA jda, String id) {
		super(name, description, permission, min, max);
		this.jda = jda;
		this.guildId = id;
	}

}
