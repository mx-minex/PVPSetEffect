package kr.minex.pvpseteffect.domain.repository;

import kr.minex.pvpseteffect.domain.entity.SetEffect;

import java.util.Collection;
import java.util.Optional;

/**
 * 세트 효과 저장소 인터페이스 (Port)
 */
public interface SetEffectRepository {

    void save(SetEffect setEffect);

    Optional<SetEffect> findById(String id);

    Optional<SetEffect> findByName(String name);

    Collection<SetEffect> findAll();

    void delete(String id);

    void deleteByName(String name);

    boolean existsByName(String name);

    int count();

    void saveAll();

    void loadAll();
}
