package md.leonis.shingler.gui.dao.test;

import md.leonis.shingler.gui.domain.test.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DictionaryDAO extends JpaRepository<Dictionary, Long> {

}
