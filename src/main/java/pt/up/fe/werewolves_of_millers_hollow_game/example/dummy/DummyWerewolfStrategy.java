package pt.up.fe.werewolves_of_millers_hollow_game.example.dummy;

import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jade.lang.acl.UnreadableException;
import pt.up.fe.werewolves_of_millers_hollow_game.actions.Actions;
import pt.up.fe.werewolves_of_millers_hollow_game.agents.Player;
import pt.up.fe.werewolves_of_millers_hollow_game.common.AgentTypes;
import pt.up.fe.werewolves_of_millers_hollow_game.common.GameStates;
import pt.up.fe.werewolves_of_millers_hollow_game.common.Operation;
import pt.up.fe.werewolves_of_millers_hollow_game.common.Strategy;
import pt.up.fe.werewolves_of_millers_hollow_game.messages.Message;
import pt.up.fe.werewolves_of_millers_hollow_game.messages.MessageTopics;
import pt.up.fe.werewolves_of_millers_hollow_game.messages.MessageTypes;

public class DummyWerewolfStrategy implements Strategy {
	Player player;

	public DummyWerewolfStrategy(Player player) {
		this.player = player;
	}

	public void config(Function<GameStates, Operation> config) {
		firstNightConfig(() -> config.apply(GameStates.FIRST_NIGHT));
		nightConfig(() -> config.apply(GameStates.NIGHT));
		dayConfig(() -> config.apply(GameStates.DAY));
	}

	private void firstNightConfig(Supplier<Operation> config) {
		Supplier<Integer> otherWerewolfesNumberSupplier = () -> player.gameSpecyfication.getPlayerTypesNumbers()
				.get(AgentTypes.WEREWOLF) - 1;

		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.FIRST_NIGHT).fromTypes(AgentTypes.MODERATOR)
				.then(aclMsg -> {
					if (otherWerewolfesNumberSupplier.get() > 0)
						Actions.sendMessage(player,
								new Message(MessageTypes.INFORM, MessageTopics.WHO_I_AM, player::getLocalName)
										.withReceivers(AgentTypes.WEREWOLF))
								.accept(aclMsg);
					else
						Actions.sendMessage(player, new Message(MessageTypes.OK, MessageTopics.FIRST_NIGHT)
								.withReceivers(AgentTypes.MODERATOR)).accept(aclMsg);
				});

		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.WHO_I_AM).fromTypes(AgentTypes.WEREWOLF)
				.then(aclMsg -> {
					String werewolfName = (String) aclMsg.getSender().getLocalName();
					player.players.put(werewolfName, AgentTypes.WEREWOLF);
				}).afterMessagesNumber(otherWerewolfesNumberSupplier).thenSend(player,
						new Message(MessageTypes.OK, MessageTopics.FIRST_NIGHT).withReceivers(AgentTypes.MODERATOR));
	}

	private void nightConfig(Supplier<Operation> config) {
		Random random = new Random();
		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.NIGHT_START).fromTypes(AgentTypes.MODERATOR)
				.then(aclMsg -> {
					try {
						String killedPlayer = (String) aclMsg.getContentObject();
						if (killedPlayer != null)
							player.players.remove(killedPlayer);
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				});

		// votes randomly for not werewolfs
		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.NIGHT_END).fromTypes(AgentTypes.MODERATOR)
				.thenReply(player, MessageTypes.OK, () -> {
					List<String> votes = player.players.entrySet().stream()
							.filter(entry -> !entry.getValue().equals(AgentTypes.WEREWOLF)).map(e -> e.getKey())
							.collect(Collectors.toList());
					if (votes.isEmpty())
						return "";
					return votes.get(random.nextInt(votes.size()));
				});
	}

	private void dayConfig(Supplier<Operation> config) {
		Random random = new Random();

		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.DAY_START).fromTypes(AgentTypes.MODERATOR)
				.then(aclMsg -> {
					try {
						String killedPlayer = (String) aclMsg.getContentObject();
						if (killedPlayer != null)
							player.players.remove(killedPlayer);
					} catch (UnreadableException e) {
						e.printStackTrace();
					}
				});
		config.get().whenReceive(MessageTypes.INFORM, MessageTopics.DAY_END).fromTypes(AgentTypes.MODERATOR)
				.thenSend(player, new Message(MessageTypes.OK, MessageTopics.DAY_END, () -> {
					List<String> votes = player.players.entrySet().stream()
							.filter(entry -> !entry.getValue().equals(AgentTypes.WEREWOLF)).map(e -> e.getKey())
							.collect(Collectors.toList());
					if (votes.isEmpty())
						return "";
					return votes.get(random.nextInt(votes.size()));
				}).withReceivers(AgentTypes.ALL));

	}
}
