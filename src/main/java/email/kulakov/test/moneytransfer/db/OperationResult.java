package email.kulakov.test.moneytransfer.db;

public enum OperationResult {
    Success, NoAccount, NoMoney;

    public boolean isSuccess() {
        return this == Success;
    }
}
