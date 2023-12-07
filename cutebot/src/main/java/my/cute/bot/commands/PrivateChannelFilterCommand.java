package my.cute.bot.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import my.cute.bot.preferences.wordfilter.FilterResponseAction;
import my.cute.bot.preferences.wordfilter.WordFilter;
import my.cute.bot.util.MiscUtils;
import my.cute.bot.util.RegexValidator;
import my.cute.bot.util.StandardMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
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
 * <b>add</b>: args should be a comma-separated list of words to add to the filter, surrounded by 
 * quotation marks. adds those
 * words in addition to whatever words already exist in the filter<br>
 * <b>remove</b>: args should be a comma-separated list of words to remove from the filter, surrounded
 * by quotation marks. removes
 * any words present in the list from the filter, leaving the remainder<br>
 * <b>clear</b>: no args. clears the filter, removing all words and turning regex mode off if it was on. after this, the filter won't match
 * on anything. note this does not change the set filter response actions<br>
 * <b>set</b>: args should be a comma-separated list of words to use for the filter, or a regex string
 * if the filter is in regex mode (see regex mode); in either case args should be surrounded by 
 * quotation marks. discards any words that existed in the 
 * filter prior to executing the command<br>
 * <b>regex</b>: args should either be <code>on</code> or <code>off</code>. <code>on</code> switches the 
 * filter to regex mode - add and
 * remove will no longer have any effect and the set list of words will be ignored. instead, 
 * an explicit regex filter can be set via "set" mode, for more advanced filter use<br>
 * <b>action</b>: args should be a single word consisting only of numbers - each number indicates a
 * filter response action to use. valid numbers are 1: don't process the message, 2: send a message
 * in the guild indicating that the wordfilter was triggered, 3: send a private message indicating
 * that the wordfilter was triggered, 4: delete the message, 5: apply the specified role to the user,
 * 6: kick the user, 7: ban the user<br>
 * <b>role</b>: args should be the id of the role to apply to users who trigger the filter, for use
 * with action 5 above<br>
 * <b>view</b>: no args. view the current filter (the word list if in basic mode, the regex
 * if in regex mode) and the current filter response actions
 *
 */
public class PrivateChannelFilterCommand extends PrivateChannelCommandTargeted {

	private final static Logger logger = LoggerFactory.getLogger(PrivateChannelFilterCommand.class);
	final static String NAME = "filter";
	private final static String DESCRIPTION = "view or modify a server's wordfilter";
	private final static EmbedBuilder HELP = MiscUtils.applyFlair(new EmbedBuilder()
			.setTitle(NAME)
			.setDescription("view or modify a server's wordfilter. this is basically a list of "
					+ "blacklisted words or phrases along with a set of actions to take when a user sends a "
					+ "message containing one of those words. actions include general moderation "
					+ "tools (for example, deleting the message) along with cutebot-specific tools "
					+ "(for example, ignoring the message so cutebot won't potentially repeat it). a "
					+ "server's wordfilter may contain up to " + WordFilter.MAX_FILTERED_WORDS + " words, "
					+ "and each word may be at most " + WordFilter.MAX_WORD_LENGTH + " characters long. "
					+ "note that because wordfilters are saved on a per-server basis, this command "
					+ "**requires a target server** (see `!help default` for more on ways to provide "
					+ "a target server)")
			.addField("use:", "`!filter <mode> [<options>] [<target server>]", false)
			.addField("modes", "`add`: `<options>` should be a comma-separated list of words or phrases "
					+ "to add to the wordfilter, all surrounded by quotation marks"
					+ System.lineSeparator()
					+ "`remove`: `<options>` should be a comma-separated list of words or phrases to "
					+ "remove from the wordfilter, all surrounded by quotation marks"
					+ System.lineSeparator()
					+ "`clear`: no `<options>`. clears the filter, removing all words and disabling regex "
					+ "mode if it was enabled (see `regex` below). note this will have no effect on the currently "
					+ "set filter response actions (see `action` below)"
					+ System.lineSeparator()
					+ "`set`: `<options>` should be a comma-separated list of words or phrases to use for "
					+ "the filter, or a regex string to use as the filter if regex mode is enabled (see `regex` "
					+ "below); in either case, `<options>` should be surrounded by quotation marks. note this "
					+ "discards any currently filtered words and replaces them with whatever words are provided "
					+ "by this command"
					+ System.lineSeparator()
					+ "`regex`: `<options>` should be either the word `on` to enable regex mode, or the word `off` "
					+ "to disable regex mode (no quotation marks in either case). when regex mode is enabled, `add` "
					+ "and `remove` will have no effect, and the current list of filtered words will be ignored. "
					+ "instead, an explicit regex string can be set as the filter by using `set`. this is for advanced "
					+ "filter use; if you don't know what any of this means then ignore it"
					+ System.lineSeparator()
					+ "`action`: `<options>` should be a single word consisting only of numbers from 1-7. each number "
					+ "corresponds to a different action that will be taken when the wordfilter is triggered, and all "
					+ "actions included will be taken. the possible actions and their numbers are 1: don't process the "
					+ "message (this will prevent cutebot from repeating it), 2: send a message in the server indicating "
					+ "that the offending user triggered the wordfilter, 3: send a private message to the offending user "
					+ "to inform them that they triggered it, 4: delete the message, 5: apply a given role to "
					+ "the offending user, 6: kick the offending user, 7: ban the offending user"
					+ System.lineSeparator()
					+ "`role`: `<options>` should be the ID of a role to apply to a user who triggers the wordfilter, "
					+ "for use with action 5 as stated above (see `action`)"
					+ System.lineSeparator()
					+ "`view`: no `<options>`. displays the current wordfilter settings", false)
			.addField("examples", "`!filter add \"cute, manly butts\" 1111111111`"
					+ System.lineSeparator()
					+ "adds the phrases `cute` and `manly butts` to the list of filtered words for server ID `1111111111`"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!filter remove \"very,nice,friendly,kind people\" \"cute server\"`"
					+ System.lineSeparator()
					+ "removes the phrases `very`, `nice`, `friendly`, and `kind people` from the list of filtered words "
					+ "for server name `cute server`"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!filter clear`"
					+ System.lineSeparator()
					+ "clears the wordfilter for your default server (see `!help default`)"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!filter set \"flying,goose\"`"
					+ System.lineSeparator()
					+ "sets the phrases `flying` and `goose` as the only two filtered words for your default server "
					+ "(see `!help default`)"
					+ System.lineSeparator()
					+ System.lineSeparator()
					+ "`!filter action 124 3333333333`"
					+ System.lineSeparator()
					+ "sets the actions to be taken when the wordfilter is triggered in server ID `3333333333` to: "
					+ "skip processing the message, send a notifying message in the server, delete the message", false));
			
	private final static Pattern ACTION_FLAGS = Pattern.compile("[1234567]+");
	
	private final Map<String, WordFilter> allFilters;
	
	PrivateChannelFilterCommand(Map<String, WordFilter> filters) {
		/*
		 * practically speaking the max params is like, 3, but if arbitrary regex is allowed
		 * then someone should be able to use a regex with whatever amount of spaces they
		 * want, so we do this. targetguild can still be obtained if its provided as the 
		 * last parameter or if a default guild is set
		 */
		super(NAME, DESCRIPTION, HELP, PermissionLevel.ADMIN, 1, Integer.MAX_VALUE);
		this.allFilters = filters;
	}

	@Override
	public void execute(Message message, String[] params, Guild targetGuild) {
		WordFilter filter = this.allFilters.get(targetGuild.getId());
		//should only have valid targetguild, but make check anyway
		if(filter == null) {
			logger.warn(this + ": guild '" + targetGuild + "' had no mapped wordfilter? msg: " + message);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
			return;
		}
		
		try {
			if(params[1].equalsIgnoreCase("add")) {
				if(params.length >= 3) {
					String[] wordsToAdd = MiscUtils.parseWordsToFilter(message.getContentRaw());
					if(wordsToAdd.length != 0) {
						if(filter.add(wordsToAdd)) {
							message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
						} else {
							message.getChannel().sendMessage("wordfilter was not changed! check your parameters "
									+ "(wordfilter at capacity? trying to add words already present? "
									+ "words too long? mode set to regex?)").queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.failedToFindWordfilterWords()).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("remove")) {
				if(params.length >= 3) {
					String[] wordsToRemove = MiscUtils.parseWordsToFilter(message.getContentRaw());
					if(wordsToRemove.length != 0) {
						if(filter.remove(wordsToRemove)) {
							message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
						} else {
							message.getChannel().sendMessage("wordfilter was not changed! check your parameters "
									+ "(trying to remove words that aren't part of the filter? mode set to regex?)").queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.failedToFindWordfilterWords()).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("clear")) {
				filter.clear();
				message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
			} else if (params[1].equalsIgnoreCase("set")) {
				if(params.length >= 3) {
					String filterString = MiscUtils.extractQuotationMarks(message);
					if(filterString != null) {
						String previousFilter = filter.get();
						try {
							filter.set(filterString);
							RegexValidator.regexTimeoutTestAsync(Pattern.compile(filterString, Pattern.CASE_INSENSITIVE))
								.whenCompleteAsync((result, throwable) -> {
									if(throwable != null) {
										message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
										logger.warn("unknown exception during regex timeout test: " + throwable);
										throwable.printStackTrace();
										try {
											filter.set(previousFilter);
										} catch (IOException e) {
											throw new UncheckedIOException(e);
										}
									} else {
										if(result == true) {
											//validation success
											message.getChannel().sendMessage(StandardMessages.wordfilterModified()).queue();
										} else {
											//validation fail. restore previous filter
											//TODO maybe should be a bit more lenient somehow with timeouts here? idk
											//or could re-enable wordfilter during next maintenance or something so it's not permanent
											String responseMessage;
											try {
												filter.set(previousFilter);
												filter.addStrike();
												if(filter.getStrikes() >= WordFilter.getStrikesToDisable()) {
													filter.setEnabled(false);
													responseMessage = StandardMessages.wordfilterDisabled(targetGuild);
												} else {
													responseMessage = StandardMessages.wordfilterStrike(targetGuild, filter.getType());
												}
												message.getChannel().sendMessage(responseMessage).queue();
											} catch (IOException e) {
												throw new UncheckedIOException(e);
											}
										}
									}
								});
						} catch (PatternSyntaxException e) {
							message.getChannel().sendMessage("error: invalid regex. wordfilter was not modified").queue();
						}
					} else {
						message.getChannel().sendMessage(StandardMessages.failedToFindWordfilterWords()).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("regex")) {
				if(params.length >= 3) {
					if(params[2].equalsIgnoreCase("on")) {
						filter.setType(WordFilter.Type.REGEX);
						message.getChannel().sendMessage("wordfilter has been set to regex mode. set a regex with !filter set <regex>").queue();
					} else if (params[2].equalsIgnoreCase("off")) {
						filter.setType(WordFilter.Type.BASIC);
						message.getChannel().sendMessage("wordfilter set to default mode").queue();
					} else {
						message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("action")) {
				if(params.length >= 3) {
					if(ACTION_FLAGS.matcher(params[2]).matches()) {
						filter.setActions(FilterResponseAction.numbersToActions(params[2]));
						message.getChannel().sendMessage("wordfilter actions have been set to: " + filter.getActions().stream().map(action 
								-> FilterResponseAction.toDescription(action)).collect(Collectors.joining(", "))).queue();
					} else {
						message.getChannel().sendMessage("invalid actions '" + params[2] + "' (use only numbers 1-7)").queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("role")) {
				if(params.length >= 3) {
					//TODO option to clear set role
					Role newRole = MiscUtils.parseRole(targetGuild, message, 2);
					if(newRole != null) {
						filter.setRoleId(newRole.getId());
						message.getChannel().sendMessage("set role '" + newRole.getName() + "' (id=" + newRole.getId()
								+ ") as role to apply when wordfilter is triggered").queue();
						if(!filter.getActions().contains(FilterResponseAction.ROLE)) {
							message.getChannel().sendMessage("(note wordfilter is not currently set to apply role when "
									+ "filter is triggered)").queue();
						}
					} else {
						message.getChannel().sendMessage("no matching role found in your message. try "
								+ "putting the role's name or id in quotation marks").queue();
					}
				} else {
					message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
				}
			} else if (params[1].equalsIgnoreCase("view")) {
				MiscUtils.sendMessages(message.getChannel(), this.getFormattedWordfilterMessages(filter, targetGuild));
			} else {
				message.getChannel().sendMessage(StandardMessages.invalidSyntax(NAME)).queue();
			}
		} catch (UncheckedIOException e) {
			logger.warn(this + ": unknown IOException during command execution. msg: " + message, e.getCause());
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		} catch (IOException e) {
			logger.warn(this + ": unknown IOException during command execution. msg: " + message, e);
			message.getChannel().sendMessage(StandardMessages.unknownError()).queue();
		}
	}
	
	private Queue<Message> getFormattedWordfilterMessages(WordFilter filter, Guild guild) {
		MessageBuilder builder = new MessageBuilder();
		builder.append("wordfilter for server " + MiscUtils.getGuildString(guild));
		builder.append(System.lineSeparator());
		builder.append(System.lineSeparator());
		builder.append("mode: " + filter.getType().name().toLowerCase());
		builder.append(System.lineSeparator());
		if(filter.getType() == WordFilter.Type.REGEX) 
			builder.append("filter: ```");
		else /*filter.getType() == WordFilter.Type.BASIC */
			builder.append("flagged words: ```");
		String filterString = filter.get();
		if(filterString.isBlank())
			builder.append("none");
		else
			builder.append(filterString);
		builder.append("```");
		builder.append("actions taken when filter is triggered: " );
		builder.append(filter.getActions().stream().map(action 
				-> FilterResponseAction.toDescription(action)).collect(Collectors.joining(", ")));
		if(filter.getActions().contains(FilterResponseAction.ROLE)) {
			builder.append(System.lineSeparator());
			builder.append("role to apply when filter is triggered: " );
			/*
			 * possible that the role is null (eg role deleted since it was set), or no
			 * role has ever been set (filter.getRoleId() returns empty string)
			 */
			Role role;
			if(filter.getRoleId().isBlank())
				role = null;
			else
				role = guild.getRoleById(filter.getRoleId());
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
