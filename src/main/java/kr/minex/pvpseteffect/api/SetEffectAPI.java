package kr.minex.pvpseteffect.api;

import org.bukkit.entity.Player;
import kr.minex.pvpseteffect.PVPSetEffectPlugin;
import kr.minex.pvpseteffect.domain.entity.PlayerSetState;
import kr.minex.pvpseteffect.domain.entity.SetEffect;
import kr.minex.pvpseteffect.domain.vo.AbilityType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * PVPSetEffect 외부 연동 API
 *
 * 다른 플러그인에서 세트 효과 시스템과 연동할 때 사용
 *
 * <pre>
 * // 사용 예시
 * SetEffectAPI api = SetEffectAPI.getInstance();
 *
 * // 플레이어의 공격력 보너스 조회
 * double attackBonus = api.getPlayerAttackBonus(player);
 *
 * // 팀 체커 등록 (흡혈 시 같은 팀 제외)
 * api.setTeamChecker((attacker, victim) -> {
 *     return myTeamPlugin.isSameTeam(attacker, victim);
 * });
 * </pre>
 */
public final class SetEffectAPI {

    private static volatile SetEffectAPI instance;
    private static final Object LOCK = new Object();

    private SetEffectAPI() {
        // 싱글톤 - 직접 인스턴스화 방지
    }

    /**
     * API 인스턴스 반환 (Thread-safe)
     *
     * Double-checked locking 패턴 사용
     *
     * @return SetEffectAPI 싱글톤 인스턴스
     */
    public static SetEffectAPI getInstance() {
        SetEffectAPI result = instance;
        if (result == null) {
            synchronized (LOCK) {
                result = instance;
                if (result == null) {
                    instance = result = new SetEffectAPI();
                }
            }
        }
        return result;
    }

    /**
     * 플러그인 활성화 여부 확인
     */
    public boolean isEnabled() {
        return PVPSetEffectPlugin.getInstance() != null;
    }

    // === 세트 효과 조회 ===

    /**
     * 모든 세트 효과 조회
     */
    public Collection<SetEffect> getAllSetEffects() {
        checkEnabled();
        return PVPSetEffectPlugin.getInstance().getSetEffectService().getAllSetEffects();
    }

    /**
     * 이름으로 세트 효과 조회
     */
    public Optional<SetEffect> getSetEffect(String name) {
        checkEnabled();
        return PVPSetEffectPlugin.getInstance().getSetEffectService().getSetEffect(name);
    }

    /**
     * 세트 효과 존재 여부 확인
     */
    public boolean setEffectExists(String name) {
        checkEnabled();
        return PVPSetEffectPlugin.getInstance().getSetEffectService().exists(name);
    }

    // === 플레이어 상태 조회 ===

    /**
     * 플레이어의 세트 상태 조회
     */
    public PlayerSetState getPlayerState(Player player) {
        checkEnabled();
        return PVPSetEffectPlugin.getInstance()
                .getPlayerEffectService()
                .getPlayerStateOrEmpty(player.getUniqueId());
    }

    /**
     * 플레이어의 세트 상태 조회 (UUID)
     */
    public PlayerSetState getPlayerState(UUID playerId) {
        checkEnabled();
        return PVPSetEffectPlugin.getInstance()
                .getPlayerEffectService()
                .getPlayerStateOrEmpty(playerId);
    }

    // === 능력 보너스 조회 ===

    /**
     * 플레이어의 공격력 보너스
     */
    public double getPlayerAttackBonus(Player player) {
        return getPlayerState(player).getAttackBonus();
    }

    /**
     * 플레이어의 방어력 보너스
     */
    public double getPlayerDefenseBonus(Player player) {
        return getPlayerState(player).getDefenseBonus();
    }

    /**
     * 플레이어의 회피율
     */
    public double getPlayerEvasionChance(Player player) {
        return getPlayerState(player).getEvasionChance();
    }

    /**
     * 플레이어의 흡혈력
     */
    public double getPlayerLifesteal(Player player) {
        return getPlayerState(player).getLifestealAmount();
    }

    /**
     * 플레이어의 치명타 확률
     */
    public double getPlayerCriticalChance(Player player) {
        return getPlayerState(player).getCriticalChance();
    }

    /**
     * 플레이어의 치명타 데미지
     */
    public double getPlayerCriticalDamage(Player player) {
        return getPlayerState(player).getCriticalDamage();
    }

    /**
     * 플레이어의 재생력 보너스
     */
    public double getPlayerRegeneration(Player player) {
        return getPlayerState(player).getRegenerationBonus();
    }

    /**
     * 플레이어의 특정 능력 보너스
     */
    public double getPlayerAbilityBonus(Player player, AbilityType type) {
        return getPlayerState(player).getAbilityBonus(type);
    }

    // === 효과 갱신 ===

    /**
     * 플레이어의 세트 효과 강제 재계산
     */
    public void recalculatePlayer(Player player) {
        checkEnabled();
        PVPSetEffectPlugin.getInstance()
                .getPlayerEffectService()
                .recalculateAndApply(player);
    }

    /**
     * 플레이어의 세트 효과 제거
     */
    public void clearPlayerEffects(Player player) {
        checkEnabled();
        PVPSetEffectPlugin.getInstance()
                .getPlayerEffectService()
                .clearPlayerEffects(player);
    }

    // === 외부 연동 ===

    /**
     * 팀 체크 함수 등록
     * 흡혈 효과 적용 시 같은 팀인지 확인하는데 사용
     *
     * @param checker (공격자, 피해자) -> 같은 팀 여부
     */
    public void setTeamChecker(BiFunction<Player, Player, Boolean> checker) {
        checkEnabled();
        PVPSetEffectPlugin.getInstance().setTeamChecker(checker);
    }

    private void checkEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("PVPSetEffect plugin is not enabled");
        }
    }
}
