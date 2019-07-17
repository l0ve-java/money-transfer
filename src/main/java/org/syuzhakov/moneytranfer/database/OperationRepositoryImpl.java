package org.syuzhakov.moneytranfer.database;

import org.apache.ibatis.jdbc.SQL;
import org.syuzhakov.moneytranfer.error.UnexpectedException;
import org.syuzhakov.moneytranfer.model.Operation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OperationRepositoryImpl implements OperationRepository {
    private ConnectionFactory connectionFactory;

    public OperationRepositoryImpl(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Operation createOperation(Operation operation) {
        try {
            //Insert new record
            final SQL sql = new SQL()
                    .INSERT_INTO("operation")
                    .VALUES("amount", "?")
                    .VALUES("ts", "sysdate");
            if (operation.getSourceAccount() != null) {
                sql.VALUES("source_account", "?");
            }
            if (operation.getTargetAccount() != null) {
                sql.VALUES("target_account", "?");
            }

            final PreparedStatement insert = connectionFactory.getConnection().prepareStatement(sql.toString());

            int i = 0;
            insert.setLong(++i, operation.getAmount());
            if (operation.getSourceAccount() != null) {
                insert.setLong(++i, operation.getSourceAccount());
            }
            if (operation.getTargetAccount() != null) {
                insert.setLong(++i, operation.getTargetAccount());
            }
            insert.executeUpdate();
            insert.close();

            //Get inserted operation
            final Statement select = connectionFactory.getConnection().createStatement();
            final ResultSet resultSet = select.executeQuery(
                    "select id, source_account, target_account, amount, ts from operation where id = scope_identity()");
            final Operation result = mapOperation(resultSet);
            select.close();
            return result;
        } catch (SQLException e) {
            throw new UnexpectedException(e);
        }
    }

    private Operation mapOperation(ResultSet resultSet) throws SQLException {
        Operation operation = null;
        if (resultSet.next()) {
            operation = Operation.builder()
                    .id(resultSet.getLong("id"))
                    .sourceAccount((Long) resultSet.getObject("source_account"))
                    .targetAccount((Long) resultSet.getObject("target_account"))
                    .amount(resultSet.getLong("amount"))
                    .timestamp(resultSet.getTimestamp("ts").toInstant())
                    .build();
        }
        resultSet.close();
        return operation;
    }
}
