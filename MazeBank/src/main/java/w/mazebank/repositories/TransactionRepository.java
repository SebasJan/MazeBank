package w.mazebank.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import w.mazebank.models.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends BaseRepository<Transaction, Long> {
    @Query("SELECT CAST(ROUND(SUM(t.amount), 2) AS DOUBLE) as total " +
        "FROM Transaction t " +
        "JOIN t.sender s " +
        "JOIN t.receiver r " +
        "JOIN s.user u1 " +
        "JOIN r.user u2 " +
        "WHERE CAST(t.timestamp AS DATE) = CURRENT_DATE() " +
        "  AND s.id = :senderId " +
        "  AND u1.id <> u2.id")
    Double getTotalAmountOfTransactionForToday(@Param("senderId") Long senderId);

    @Override
    @Query("SELECT t FROM Transaction t WHERE t.sender.iban LIKE %?1% OR t.receiver.iban LIKE %?1%")
    List<Transaction> findBySearchString(@Param("iban") String search, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender.user.id = :sender OR t.receiver.user.id = :receiver)")
    List<Transaction> findBySenderUserIdOrReceiverUserId(@Param("sender") Long senderId, @Param("receiver") Long receiverId, Pageable pageable);


    @Query("SELECT t FROM Transaction t WHERE t.timestamp BETWEEN :startDate AND :endDate")
    List<Transaction> findByTimestampBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    List<Transaction> findBySenderIdOrReceiverId(Long senderIban, Long receiverIban, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sender.user.id = :userId OR t.receiver.user.id = :userId) AND t.timestamp BETWEEN :startDate AND :endDate AND (t.sender.iban LIKE %:iban% OR t.receiver.iban LIKE %:iban%)")
    List<Transaction> findBySenderUserIdOrReceiverUserIdAndTimestampBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("userId") long userId, @Param("iban") String iban);

}
