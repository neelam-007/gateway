package com.l7tech.server.cluster;

import com.l7tech.util.ConfigFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.l7tech.util.Config;
import com.l7tech.util.Triple;
import com.l7tech.util.ExceptionUtils;

/**
 * Elects a single node as the "master" node in the cluster.
 */
public interface ClusterMaster {

    //- PUBLIC

    boolean isMaster();
}
