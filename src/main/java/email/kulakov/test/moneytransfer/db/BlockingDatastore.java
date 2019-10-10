package email.kulakov.test.moneytransfer.db;

import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class BlockingDatastore implements Datastore {

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
        if(account == null) {
            return null;
        }
        synchronized (account) {
            return account.balance;
        }
    }

    @Override
    public OperationResult changeMoney(long accountId, BigDecimal delta) {
        final AccountModel account = storage.get(accountId);
        if (account == null) {
            return OperationResult.NoAccount;
        }
        synchronized (account) {
            return account.changeBalance(delta);
        }
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

        final AccountModel firstLock = accountFrom > accountTo ? to : from;
        final AccountModel secondLock = accountFrom > accountTo ? from : to;
        //In case of using real database, we should do this operations in one transaction.
        synchronized (firstLock) {
            synchronized (secondLock) {
                //First we should remove money - this operation can be failed, and rollback in some cases will be impossible.
                final OperationResult removeMoneyResult = from.changeBalance(amount.negate());
                if (!removeMoneyResult.isSuccess()) {
                    return removeMoneyResult;
                }
                //Amount is positive. Fail is impossible.
                to.changeBalance(amount);
            }
        }

        return OperationResult.Success;
    }


    private static final class AccountModel {

        private BigDecimal balance = BigDecimal.ZERO;

        OperationResult changeBalance(BigDecimal delta) {
            final BigDecimal newBalance = balance.add(delta);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                return OperationResult.NoMoney;
            }

            balance = newBalance;

            return OperationResult.Success;
        }
    }
}

