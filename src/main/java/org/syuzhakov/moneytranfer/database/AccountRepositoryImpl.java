package org.syuzhakov.moneytranfer.database;

import org.apache.ibatis.jdbc.SQL;
import org.syuzhakov.moneytranfer.error.BadRequestException;
import org.syuzhakov.moneytranfer.error.UnexpectedException;
import org.syuzhakov.moneytranfer.model.Account;
import org.syuzhakov.moneytranfer.model.AccountStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class AccountRepositoryImpl implements AccountRepository {
    private ConnectionFactory connectionFactory;

    public AccountRepositoryImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Account createNewAccount(Account account) {
        try {
            //Insert new record
            final PreparedStatement insert = connectionFactory.getConnection().prepareStatement(new SQL()
                    .INSERT_INTO("account")
                    .VALUES("id", "SQ_ACCOUNT.NEXTVAL")
                    .VALUES("status", "?")
                    .VALUES("fd", "sysdate")
                    .VALUES("td", "parsedatetime('9999-01-01', 'yyyy-MM-dd', 'en', 'GMT')")
                    .toString());
            insert.setInt(1, account.getStatus().getValue());
            insert.executeUpdate();
            insert.close();

            //Get inserted account
            final Statement select = connectionFactory.getConnection().createStatement();
            final ResultSet resultSet = select
                    .executeQuery("select id, status, sysdate as ts from account where n = scope_identity()");
            final Account result = mapAccount(resultSet);
            select.close();
            return result;
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public Account getAccountById(long id, boolean forUpdate) {
        try {
            String sql = "select id, status, sysdate as ts from account where id = ? and sysdate between fd and td";
            if (forUpdate) {
                sql += " for update";
            }
            final PreparedStatement select = connectionFactory.getConnection().prepareStatement(sql);
            select.setLong(1, id);
            final ResultSet resultSet = select.executeQuery();
            final Account result = mapAccount(resultSet);
            select.close();
            return result;
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }


    @Override
    public void updateAccount(Account account) {
        try {
            //Lock current record
            final Account lockedAccount = getAccountById(account.getId(), true);
            if (lockedAccount == null) {
                throw new BadRequestException("Account doesn't exist: " + account.getId());
            }
            final Timestamp lockTime = Timestamp.from(lockedAccount.getActuality());

            //Close current record
            final PreparedStatement update = connectionFactory.getConnection().prepareStatement(new SQL()
                    .UPDATE("account")
                    .SET("td = ?")
                    .WHERE("id = ?")
                    .WHERE("? between fd and td")
                    .toString());
            update.setTimestamp(1, lockTime);
            update.setLong(2, account.getId());
            update.setTimestamp(3, lockTime);
            update.executeUpdate();
            update.close();

            //Insert new record
            final PreparedStatement insert = connectionFactory.getConnection().prepareStatement(new SQL()
                    .INSERT_INTO("account")
                    .VALUES("id", "?")
                    .VALUES("status", "?")
                    .VALUES("fd", "?")
                    .VALUES("td", "parsedatetime('9999-01-01', 'yyyy-MM-dd', 'en', 'GMT')")
                    .toString());
            insert.setLong(1, account.getId());
            insert.setInt(2, account.getStatus().getValue());
            insert.setTimestamp(3, lockTime);
            insert.executeUpdate();
            insert.close();
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        Account account = null;
        if (resultSet.next()) {
            account = Account.builder()
                    .id(resultSet.getLong("id"))
                    .status(AccountStatus.fromValue(resultSet.getInt("status")))
                    .actuality(resultSet.getTimestamp("ts").toInstant())
                    .build();
        }
        resultSet.close();
        return account;
    }
}
