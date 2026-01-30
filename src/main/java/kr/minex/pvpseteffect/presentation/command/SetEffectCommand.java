package kr.minex.pvpseteffect.presentation.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import kr.minex.pvpseteffect.application.service.SetEffectService;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.AbilityType;
import kr.minex.pvpseteffect.domain.vo.PotionType;
import kr.minex.pvpseteffect.domain.vo.SetBonus;
import kr.minex.pvpseteffect.infrastructure.config.MessageConfig;
import kr.minex.pvpseteffect.presentation.gui.SetEffectGUI;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * /세트효과 명령어 처리기
 *
 * 모든 입력값에 대해 검증을 수행하여 인젝션 공격을 방지합니다.
 */
public class SetEffectCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "seteffect.admin";
    private static final int MAX_SET_NAME_LENGTH = 32;
    private static final java.util.regex.Pattern VALID_NAME_PATTERN =
            java.util.regex.Pattern.compile("^[가-힣a-zA-Z0-9_-]+$");

    private final SetEffectService setEffectService;
    private final SetEffectGUI setEffectGUI;

    public SetEffectCommand(SetEffectService setEffectService, SetEffectGUI setEffectGUI) {
        this.setEffectService = setEffectService;
        this.setEffectGUI = setEffectGUI;
    }

    /**
     * 세트 이름 유효성 검증
     *
     * @param name 검증할 이름
     * @return 유효한 경우 true
     */
    private boolean isValidSetName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (name.length() > MAX_SET_NAME_LENGTH) {
            return false;
        }
        return VALID_NAME_PATTERN.matcher(name).matches();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(MessageConfig.ERROR_NO_PERMISSION);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "제작":
            case "create":
                handleCreate(sender, args);
                break;
            case "삭제":
            case "delete":
                handleDelete(sender, args);
                break;
            case "목록":
            case "list":
                handleList(sender);
                break;
            case "설정":
            case "config":
                handleConfig(sender, args);
                break;
            case "능력":
            case "ability":
                handleAbility(sender, args);
                break;
            case "포션":
            case "potion":
                handlePotion(sender, args);
                break;
            case "능력보기":
            case "view":
                handleView(sender, args);
                break;
            case "능력목록":
            case "abilitylist":
                handleAbilityList(sender);
                break;
            case "포션목록":
            case "potionlist":
                handlePotionList(sender);
                break;
            case "삭제보너스":
            case "removebonus":
                handleRemoveBonus(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            sender.sendMessage(MessageConfig.ERROR_NAME_NO_SPACE);
            return;
        }

        String name = args[1];

        if (name.contains(" ")) {
            sender.sendMessage(MessageConfig.ERROR_NAME_NO_SPACE);
            return;
        }

        if (!isValidSetName(name)) {
            sender.sendMessage(MessageConfig.ERROR_NAME_INVALID);
            return;
        }

        try {
            setEffectService.createSetEffect(name);
            sender.sendMessage(MessageConfig.format(MessageConfig.SET_CREATED, name));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MessageConfig.ERROR_SET_EXISTS);
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }

        String name = args[1];

        if (setEffectService.deleteSetEffect(name)) {
            sender.sendMessage(MessageConfig.format(MessageConfig.SET_DELETED, name));
        } else {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
        }
    }

    private void handleList(CommandSender sender) {
        Collection<SetEffect> sets = setEffectService.getAllSetEffects();

        if (sets.isEmpty()) {
            sender.sendMessage(MessageConfig.SET_LIST_EMPTY);
            return;
        }

        sender.sendMessage(MessageConfig.SET_LIST_HEADER);
        int index = 1;
        for (SetEffect set : sets) {
            sender.sendMessage(MessageConfig.format(MessageConfig.SET_LIST_ITEM, index++, set.getName()));
        }
    }

    private void handleConfig(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageConfig.ERROR_PLAYER_ONLY);
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }

        String name = args[1];
        Optional<SetEffect> setEffect = setEffectService.getSetEffect(name);

        if (setEffect.isEmpty()) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
            return;
        }

        setEffectGUI.openConfigGUI((Player) sender, setEffect.get());
    }

    private void handleAbility(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_REQUIRED);
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(MessageConfig.ERROR_ABILITY_REQUIRED);
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(MessageConfig.ERROR_VALUE_REQUIRED);
            return;
        }

        String name = args[1];

        if (!setEffectService.exists(name)) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
            return;
        }

        int pieces;
        try {
            pieces = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_NUMBER);
            return;
        }

        if (pieces < 1 || pieces > 5) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_INVALID);
            return;
        }

        AbilityType abilityType = AbilityType.fromKoreanName(args[3]);
        if (abilityType == null) {
            sender.sendMessage(MessageConfig.ERROR_ABILITY_INVALID);
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageConfig.ERROR_VALUE_NUMBER);
            return;
        }

        try {
            setEffectService.setAbilityBonus(name, pieces, abilityType, value);
            sender.sendMessage(MessageConfig.format(MessageConfig.ABILITY_SET,
                    name, pieces, abilityType.getKoreanName(), value));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
        }
    }

    private void handlePotion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_REQUIRED);
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(MessageConfig.ERROR_POTION_REQUIRED);
            return;
        }
        if (args.length < 5) {
            sender.sendMessage(MessageConfig.ERROR_VALUE_REQUIRED);
            return;
        }

        String name = args[1];

        if (!setEffectService.exists(name)) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
            return;
        }

        int pieces;
        try {
            pieces = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_NUMBER);
            return;
        }

        if (pieces < 1 || pieces > 5) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_INVALID);
            return;
        }

        PotionType potionType = PotionType.fromKoreanName(args[3]);
        if (potionType == null) {
            sender.sendMessage(MessageConfig.ERROR_POTION_INVALID);
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageConfig.ERROR_VALUE_NUMBER);
            return;
        }

        try {
            setEffectService.setPotionBonus(name, pieces, potionType.getBukkitType(), value);
            sender.sendMessage(MessageConfig.format(MessageConfig.POTION_SET,
                    name, pieces, potionType.getKoreanName(), value));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
        }
    }

    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }

        String name = args[1];
        Optional<SetEffect> setEffectOpt = setEffectService.getSetEffect(name);

        if (setEffectOpt.isEmpty()) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
            return;
        }

        SetEffect setEffect = setEffectOpt.get();
        sender.sendMessage(MessageConfig.format(MessageConfig.BONUS_VIEW_HEADER, name));

        for (int i = 1; i <= 5; i++) {
            SetBonus bonus = setEffect.getBonus(i);
            if (bonus != null) {
                String bonusStr = bonus.toString();
                int idx = bonusStr.indexOf("]");
                sender.sendMessage(MessageConfig.format(MessageConfig.BONUS_VIEW_ITEM, i,
                        idx >= 0 ? bonusStr.substring(idx + 2) : bonusStr));
            } else {
                sender.sendMessage(MessageConfig.format(MessageConfig.BONUS_VIEW_EMPTY, i));
            }
        }
    }

    private void handleAbilityList(CommandSender sender) {
        sender.sendMessage(MessageConfig.ABILITY_LIST_HEADER);
        sender.sendMessage(MessageConfig.format(MessageConfig.ABILITY_LIST, AbilityType.getAllKoreanNames()));
        sender.sendMessage(MessageConfig.ABILITY_LIST_NOTE1);
        sender.sendMessage(MessageConfig.ABILITY_LIST_NOTE2);
    }

    private void handlePotionList(CommandSender sender) {
        sender.sendMessage(MessageConfig.POTION_LIST_HEADER);
        sender.sendMessage(MessageConfig.format(MessageConfig.POTION_LIST, PotionType.getAllKoreanNames()));
    }

    private void handleRemoveBonus(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageConfig.ERROR_NAME_REQUIRED);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_REQUIRED);
            return;
        }

        String name = args[1];

        if (!setEffectService.exists(name)) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
            return;
        }

        int pieces;
        try {
            pieces = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_NUMBER);
            return;
        }

        if (pieces < 1 || pieces > 5) {
            sender.sendMessage(MessageConfig.ERROR_PIECES_INVALID);
            return;
        }

        try {
            setEffectService.removeBonus(name, pieces);
            sender.sendMessage(MessageConfig.format(MessageConfig.BONUS_REMOVED, name, pieces));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MessageConfig.ERROR_SET_NOT_FOUND);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageConfig.HELP_HEADER);
        sender.sendMessage(MessageConfig.HELP_TITLE);
        sender.sendMessage("");
        sender.sendMessage(MessageConfig.HELP_CREATE);
        sender.sendMessage(MessageConfig.HELP_DELETE);
        sender.sendMessage(MessageConfig.HELP_LIST);
        sender.sendMessage(MessageConfig.HELP_CONFIG);
        sender.sendMessage("");
        sender.sendMessage(MessageConfig.HELP_ABILITY);
        sender.sendMessage(MessageConfig.HELP_ABILITY_EX);
        sender.sendMessage(MessageConfig.HELP_POTION);
        sender.sendMessage(MessageConfig.HELP_POTION_EX);
        sender.sendMessage("");
        sender.sendMessage(MessageConfig.HELP_VIEW);
        sender.sendMessage(MessageConfig.HELP_ABILITY_LIST);
        sender.sendMessage(MessageConfig.HELP_POTION_LIST);
        sender.sendMessage(MessageConfig.HELP_REMOVE_BONUS);
        sender.sendMessage("");
        sender.sendMessage(MessageConfig.HELP_HEADER);
        sender.sendMessage(MessageConfig.HELP_FOOTER);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                    "제작", "삭제", "목록", "설정", "능력", "포션", "능력보기", "능력목록", "포션목록", "삭제보너스"
            ));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("삭제", "설정", "능력", "포션", "능력보기", "삭제보너스").contains(subCommand)) {
                completions.addAll(setEffectService.getAllSetEffects().stream()
                        .map(SetEffect::getName)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (Arrays.asList("능력", "포션", "삭제보너스").contains(subCommand)) {
                completions.addAll(IntStream.rangeClosed(1, 5)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.toList()));
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            if ("능력".equals(subCommand)) {
                completions.addAll(Arrays.stream(AbilityType.values())
                        .map(AbilityType::getKoreanName)
                        .collect(Collectors.toList()));
            } else if ("포션".equals(subCommand)) {
                completions.addAll(Arrays.stream(PotionType.values())
                        .map(PotionType::getKoreanName)
                        .collect(Collectors.toList()));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
