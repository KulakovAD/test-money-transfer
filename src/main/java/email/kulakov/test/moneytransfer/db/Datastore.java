package email.kulakov.test.moneytransfer.db;

import java.math.BigDecimal;

public interface Datastore {
    long createAccount();
    BigDecimal balance(long accountId);
    OperationResult changeMoney(long accountId, BigDecimal delta);
    OperationResult transferMoney(long accountFrom, long accountTo, BigDecimal amount);
}
