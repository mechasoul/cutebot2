package my.cute.bot.commands;

import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.MyListener;
import my.cute.bot.preferences.wordfilter.FilterResponseAction;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

/**
 * for managing a server's wordfilter, an object that can be configured to perform
 * some collection of actions when a user says any of a collection of flagged phrases
 * <p>
 * <b>use</b>: !filter mode args [guild id]
 * <p>
 * <b>modes</b>: add, remove, clear, set, regex, action, role, view
 * <p>
 * <b>add</b>: args should be a comma-separated list of words to add to the filter. adds those
 * words in addition to whatever words already exist in the filter<br>
 * <b>remove</b>: args should be a comma-separated list of words to remove from the filter. removes
 * any words present in the list from the filter, leaving the remainder<br>
 * <b>clear</b>: no args. clears the filter, removing all words. after this, the filter won't match
 * on anything. note this does not change the set filter response actions<br>
 * <b>set</b>: args should be a comma-separated list of words to use for the filter, or a regex string
 * if the filter is in regex mode (see regex mode). discards any words that existed in the 
 * filter prior to executing the command
 * <b>regex</b>: args should either be "on" or "off". "on" switches the filter to regex mode - add and
 * remove will no longer have any effect and the set list of words will be ignored. instead, 
 * an explicit regex filter can be set via "set" mode, for more advanced filter use<br>
 * <b>action</b>: args should be a single word consisting only of numbers - each number indicates a
 * filter response action to use TODO put mapping here<br>
 * <b>role</b>: args should be the id of the role to apply to users who trigger the filter<br>
 * <b>view</b>: no args. view the current filter (the word list if in basic mode, the regex
 * if in regex mode) and the current filter response actions
 *
 */
public class PrivateChannelFilterCommand extends PrivateChannelCommandTargeted {

	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelFilterCommand.class);
	final static String NAME = "filter";
	private final static Pattern ACTION_FLAGS = Pattern.compile("[1234567]+");
	
	private final Map<String, WordFilter> allFilters;
	private final MyListener bot;
	private final JDA jda;
	
	PrivateChannelFilterCommand(Map<String, WordFilter> filters, MyListener bot, JDA jda) {
		/*
		 * practically speaking the max params is like, 3, but if arbitrary regex is allowed
		 * then someone should be able to use a regex with whatever amount of spaces they
		 * want, so we do this. TODO can i still get the targetguild if i do that?
		 */
		super(NAME, PermissionLevel.ADMIN, 1, Integer.MAX_VALUE);
		this.allFilters = filters;
		this.bot = bot;
		this.jda = jda;
	}

	@Override
	public void execute(Message message, String[] params, String targetGuild) {
		WordFilter filter = this.allFilters.get(targetGuild);
		//should only have valid targetguild, but make check anyway
		if(filter == null) {
			logger.warn(this + ": guild id '" + targetGuild + "' had no mapped wordfilter? msg: " + message);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			return;
		}
		
		try {
			if(params[1].equals("add")) {
				if(params.length >= 3) {
					if(filter.add(params[2].split(","))) {
						message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
					} else {
						message.getChannel().sendMessage("wordfilter was not changed! check your parameters "
								+ "(wordfilter at capacity? trying to add words already present? "
								+ "mode set to regex?)").queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("remove")) {
				if(params.length >= 3) {
					if(filter.remove(params[2].split(","))) {
						message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
					} else {
						message.getChannel().sendMessage("wordfilter was not changed! check your parameters "
								+ "(trying to remove words that aren't part of the filter? mode set to regex?)").queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("clear")) {
				filter.clear();
				message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
			} else if (params[1].equals("set")) {
				if(params.length >= 3) {
					if(filter.getType() == WordFilter.Type.REGEX) {
						//TODO regex validation here
					} else {
						filter.set(params[2]);
						message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("regex")) {
				if(params.length >= 3) {
					if(params[2].equals("on")) {
						filter.setType(WordFilter.Type.REGEX);
						message.getChannel().sendMessage("wordfilter has been set to regex mode. set a regex with !filter set <regex>").queue();
					} else if (params[2].equals("off")) {
						filter.setType(WordFilter.Type.BASIC);
						message.getChannel().sendMessage("wordfilter set to default mode").queue();
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("action")) {
				if(params.length >= 3) {
					if(ACTION_FLAGS.matcher(params[2]).matches()) {
						filter.setActions(FilterResponseAction.numbersToActions(params[2]));
						message.getChannel().sendMessage("wordfilter actions successfully modified").queue();
					} else {
						message.getChannel().sendMessage("invalid actions '" + params[2] + "' (use only numbers 1-7)").queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("role")) {
				if(params.length >= 3) {
					Role newRole = this.jda.getGuildById(targetGuild).getRoleById(params[2]);
					if(newRole != null) {
						filter.setRoleId(params[2]);
						message.getChannel().sendMessage("set role '" + newRole.getName() + "' (id=" + newRole.getId()
								+ ") as role to apply when wordfilter is triggered").queue();
						if(!filter.getActions().contains(FilterResponseAction.ROLE)) {
							message.getChannel().sendMessage("(note wordfilter is not currently set to apply role when "
									+ "filter is triggered)").queue();
						}
					} else {
						message.getChannel().sendMessage("no role found with id '" + params[2] 
								+ "' in server '" + this.bot.getGuildString(targetGuild)).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equals("view")) {
				this.getFormattedWordfilterView(filter).forEach(builtMsg -> message.getChannel().sendMessage(builtMsg).queue());
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution. msg: " + message, e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	private Queue<Message> getFormattedWordfilterView(WordFilter filter) {
		MessageBuilder builder = new MessageBuilder();
		builder.append("wordfilter for server '" + this.bot.getGuildString(filter.getId()) + "'");
		builder.append(System.lineSeparator());
		builder.append(System.lineSeparator());
		builder.append("mode: " + filter.getType().name().toLowerCase());
		builder.append(System.lineSeparator());
		if(filter.getType() == WordFilter.Type.REGEX) {
			builder.append("filter: " + filter.get());
		} else {
			builder.append("flagged words: ```");
			builder.append(filter.get());
			builder.append("```");
		}
		builder.append(System.lineSeparator());
		builder.append("actions taken when filter is triggered: " );
		builder.append(String.join(", ", filter.getActions().stream().map(action 
				-> FilterResponseAction.toDescription(action)).collect(Collectors.toList())));
		if(filter.getActions().contains(FilterResponseAction.ROLE)) {
			builder.append(System.lineSeparator());
			builder.append("role to apply when filter is triggered: " );
			//guild should never be null unless something really weird happens
			//possible that the role is null (eg role deleted since id was set)
			Role role = this.jda.getGuildById(filter.getId()).getRoleById(filter.getRoleId());
			if(role != null) {
				builder.append(role.getName());
				builder.append(" (id=");
				builder.append(filter.getRoleId());
				builder.append(")");
			} else {
				builder.append("none set!");
			}
		}
		return builder.buildAll();
	}

	@Override
	public String toString() {
		return "PrivateChannelFilterCommand";
	}
}
