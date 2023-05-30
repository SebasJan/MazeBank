package w.mazebank.repositories;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;
    import w.mazebank.models.Account;

    import java.util.List;
    import java.util.Optional;

@Repository
public interface AccountRepository extends BaseRepository<Account, Long> {
    Optional<Account> findByIban(String iban);

    @Override
    @Query("SELECT a FROM Account a WHERE a.iban LIKE %?1%")
    List<Account> findBySearchString(String search, Pageable pageable);

    // create a query to select accounts by first and or lastname
    @Query("SELECT a FROM Account a WHERE a.user.firstName LIKE %:name% OR a.user.lastName LIKE %:name%")
    List<Account> findAccountByName(@Param("name") String name);
}