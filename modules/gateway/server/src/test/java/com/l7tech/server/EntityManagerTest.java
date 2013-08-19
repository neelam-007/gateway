package com.l7tech.server;

import com.l7tech.util.FileUtils;
import com.l7tech.util.SyspropUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/com/l7tech/server/resources/embeddedDbContext.xml",
        "/com/l7tech/server/resources/testManagerContext.xml",
        "/com/l7tech/server/resources/dataAccessContext.xml"})
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = true)
@Transactional
@Ignore
public abstract class EntityManagerTest {

    private static File tmpDir;

    //Create database in temp directory
    static {
        try {
            tmpDir = FileUtils.createTempDirectory("TMPDB", null, null, false);
            SyspropUtil.setProperty("com.l7tech.server.varDirectory", tmpDir.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                //Drop the database data
                if (tmpDir != null && tmpDir.exists()) {
                    FileUtils.deleteDir(tmpDir);
                }
            }
        });
    }

    @Autowired
    protected ApplicationContext applicationContext;
    protected Session session;

    @Before
    public void initSession() {

        if (applicationContext != null) {
            SessionFactory sessionFactory = applicationContext.getBean("sessionFactory", SessionFactory.class);
            session = sessionFactory.getCurrentSession();
        }

    }

}
