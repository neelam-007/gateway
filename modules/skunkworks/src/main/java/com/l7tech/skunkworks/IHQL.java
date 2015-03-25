/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 * @author alex
 */
public class IHQL {
    private final SessionFactory sessionFactory;

    private IHQL() {
        Configuration config = new Configuration();
        this.sessionFactory = config.buildSessionFactory();
    }

    private void doIt() throws IOException {
        String line;
        BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
        while ((line = sin.readLine()) != null) {
            System.out.print("HSQL> ");
            if ("q".equalsIgnoreCase(line)) break;
            try {
                Session s = sessionFactory.openSession();
                Query q = s.createQuery(line);
                int spos = line.indexOf(" ");
                if (spos == -1) {
                    System.out.println("Invalid command");
                    continue;
                }
                String word = line.substring(0, spos);
                if ("delete".equalsIgnoreCase(word) || "update".equalsIgnoreCase(word)) {
                    int num = q.executeUpdate();
                    System.out.println(num + " record(s) affected.");
                } else {
                    int i = 0;
                    for (Iterator j = q.iterate(); j.hasNext();) {
                        Object o = j.next();
                        System.out.print(++i + ": ");
                        if (o instanceof Object[]) {
                            Object[] objects = (Object[])o;
                            System.out.println("{");
                            for (int k = 0; k < objects.length; k++) {
                                Object object = objects[k];
                                System.out.println("    " + (k+1) + ": " + object.toString());
                            }
                            System.out.println("}");
                        } else {
                            System.out.println(o.toString());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

        }
    }

    public static void main(String[] args) throws IOException {
        IHQL me = new IHQL();
        me.doIt();
    }
}
