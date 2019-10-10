package email.kulakov.test.moneytransfer.db;

import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class ConcurrentDatastore implements Datastore {

    private final AtomicLong idGenerator = new AtomicLong(0);
    private final Map<Long, AccountModel> storage = new ConcurrentHashMap<>();

    @Override
    public long createAccount() {
        final long accountId = idGenerator.incrementAndGet();
        storage.put(accountId, new AccountModel());
        return accountId;
    }

    @Override
    public BigDecimal balance(long accountId) {
        final AccountModel account = storage.get(accountId);
        return account == null ? null : account.balance.get();
    }

    @Override
    public OperationResult changeMoney(long accountId, BigDecimal delta) {
        final AccountModel account = storage.get(accountId);
        if (account == null) {
            return OperationResult.NoAccount;
        }

        return account.changeBalance(delta);
    }


    @Override
    public OperationResult transferMoney(long accountFrom, long accountTo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return OperationResult.NoMoney;
        }

        if(accountFrom == accountTo) {
            return OperationResult.Success;
        }

        final AccountModel from = storage.get(accountFrom);
        if (from == null) {
            return OperationResult.NoAccount;
        }

        final AccountModel to = storage.get(accountTo);
        if (to == null) {
            return OperationResult.NoAccount;
        }

        //First we should remove money - this operation can be failed, and rollback in some cases will be impossible.
        final OperationResult removeMoneyResult = from.changeBalance(amount.negate());
        if (!removeMoneyResult.isSuccess()) {
            return removeMoneyResult;
        }

        //Amount is positive. Fail is impossible.
        to.changeBalance(amount);

        return OperationResult.Success;
    }


    private static final class AccountModel {

        private final AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal(0));

        OperationResult changeBalance(BigDecimal delta) {
            //In SQL we can use "UPDATE account SET balance = balance + delta WHERE balance + delta > 0" and count result rows.
            while (true) {
                final BigDecimal currentBalance = balance.get();
                final BigDecimal newBalance = currentBalance.add(delta);
                if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                    return OperationResult.NoMoney;
                }

                if (balance.compareAndSet(currentBalance, newBalance)) {
                    return OperationResult.Success;
                }
            }
        }
    }
}
