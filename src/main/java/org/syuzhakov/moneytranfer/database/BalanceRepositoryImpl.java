package org.syuzhakov.moneytranfer.database;

import org.apache.ibatis.jdbc.SQL;
import org.syuzhakov.moneytranfer.error.UnexpectedException;
import org.syuzhakov.moneytranfer.model.Balance;
import org.syuzhakov.moneytranfer.model.Operation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class BalanceRepositoryImpl implements BalanceRepository {
    private ConnectionFactory connectionFactory;

    public BalanceRepositoryImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Balance getBalance(Long accountId, boolean forUpdate) {
        try {
            String sql = new SQL().FROM("balance")
                    .SELECT("account_id, balance, operation_id, sysdate as ts")
                    .WHERE("account_id = ?")
                    .WHERE("sysdate between fd and td")
                    .toString();
            if (forUpdate) {
                sql += " for update";
            }
            final PreparedStatement select = connectionFactory.getConnection().prepareStatement(sql);
            select.setLong(1, accountId);
            final ResultSet resultSet = select.executeQuery();
            final Balance result = mapBalance(resultSet);
            select.close();
            return result;
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void updateBalance(Long accountId, Long balance, Operation operation) {
        final Timestamp updateTime = Timestamp.from(operation.getTimestamp());
        try {
            //Close current record
            final PreparedStatement update = connectionFactory.getConnection().prepareStatement(new SQL()
                    .UPDATE("balance")
                    .SET("td = ?")
                    .WHERE("account_id = ?")
                    .WHERE("sysdate between fd and td")
                    .toString());
            update.setTimestamp(1, updateTime);
            update.setLong(2, accountId);
            update.executeUpdate();
            update.close();

            //Insert new record
            final PreparedStatement insert = connectionFactory.getConnection().prepareStatement(new SQL()
                    .INSERT_INTO("balance")
                    .VALUES("account_id", "?")
                    .VALUES("balance", "?")
                    .VALUES("operation_id", "?")
                    .VALUES("fd", "?")
                    .VALUES("td", "parsedatetime('9999-01-01', 'yyyy-MM-dd', 'en', 'GMT')")
                    .toString());
            insert.setLong(1, accountId);
            insert.setLong(2, balance);
            insert.setLong(3, operation.getId());
            insert.setTimestamp(4, updateTime);
            insert.executeUpdate();
            insert.close();
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    private Balance mapBalance(ResultSet resultSet) throws SQLException {
        Balance balance = null;
        if (resultSet.next()) {
            balance = Balance.builder()
                    .account(resultSet.getLong("account_id"))
                    .operation(resultSet.getLong("operation_id"))
                    .balance(resultSet.getLong("balance"))
                    .actuality(resultSet.getTimestamp("ts").toInstant())
                    .build();
        }
        resultSet.close();
        return balance;
    }
}
