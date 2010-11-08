package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.gateway.common.resources.ResourceType;
import static com.l7tech.console.panels.GlobalResourceImportContext.*;
import static com.l7tech.console.panels.GlobalResourceImportWizard.*;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * First step for the global resources import wizard.
 */
class GlobalResourceImportSearchStep extends GlobalResourceImportWizardStepPanel {
    private static final Logger logger = Logger.getLogger( GlobalResourceImportSearchStep.class.getName());

    private JTable resourcesTable;
    private JButton clearButton;
    private JButton removeButton;
    private JButton addFromUrlButton;
    private JButton addFromFileButton;
    private JTextField directoryTextField;
    private JButton findButton;
    private JTextField patternTextField;
    private JCheckBox processSubdirectoriesCheckBox;
    private JComboBox typeComboBox;
    private JButton selectButton;
    private JPanel mainPanel;
    private JLabel totalResourcesLabel;

    private SimpleTableModel<ResourceInputSource> resourceInputTableModel;

    GlobalResourceImportSearchStep( final GlobalResourceImportWizardStepPanel next ) {
        super( "search-step", next );
        init();
    }

    private void init() {
        setLayout( new BorderLayout() );
        add(mainPanel, BorderLayout.CENTER);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        } );

        resourceInputTableModel = TableUtil.configureTable(
                resourcesTable,
                TableUtil.column(resources.getString( "column.uri" ), 40, 240, 100000, Functions.<String, ResourceInputSource>propertyTransform( ResourceInputSource.class, "uriString" ), String.class),
                TableUtil.column(resources.getString( "column.size" ), 40, 80, 180, Functions.<Long, ResourceInputSource>propertyTransform( ResourceInputSource.class, "length" ), Long.class)
        );
        resourcesTable.setModel( resourceInputTableModel );
        resourcesTable.getTableHeader().setReorderingAllowed( false );
        resourcesTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        resourcesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        Utilities.setRowSorter( resourcesTable, resourceInputTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER} );
        resourceInputTableModel.addTableModelListener( new TableModelListener(){
            @Override
            public void tableChanged( final TableModelEvent e ) {
                SwingUtilities.invokeLater( new Runnable(){
                    @Override
                    public void run() {
                        enableAndDisableComponents();
                        totalResourcesLabel.setText( Integer.toString( resourcesTable.getRowCount() ) );
                        notifyListeners();
                    }
                } );
            }
        } );

        directoryTextField.getDocument().addDocumentListener( enableDisableListener );

        typeComboBox.setModel( new DefaultComboBoxModel( ResourceType.values() ) );
        typeComboBox.setRenderer( GlobalResourcesDialog.buildResourceTypeRenderer() );
        typeComboBox.setSelectedIndex(0);

        selectButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doSelect();
            }
        } );
        findButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doFind();
            }
        } );
        addFromUrlButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAddFromUrl();
            }
        } );
        addFromFileButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAddFromFile();
            }
        } );
        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doRemove();
            }
        } );
        clearButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doClear();
            }
        } );

        enableAndDisableComponents();
    }

    private void doFind() {
        String patternText = patternTextField.getText();
        if ( patternText == null || patternText.trim().isEmpty() ) {
            patternText = "^.*\\." + ((ResourceType)typeComboBox.getSelectedItem()).getFilenameSuffix() + "$";
        }

        final Pattern pattern;
        try {
            pattern = Pattern.compile( patternText, Pattern.CASE_INSENSITIVE );
        } catch ( PatternSyntaxException e ) {
            final String message = "Invalid pattern:\n"+ ExceptionUtils.getMessage(e);
            JOptionPane.showMessageDialog(this,
                                          Utilities.getTextDisplayComponent(message, 600, 100, -1, -1),
                                          "Error Finding Resources",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            CancelableOperationDialog.doWithDelayedCancelDialog( new Callable<Void>(){
                @Override
                public Void call() throws Exception {
                    findSchemas( new File( directoryTextField.getText()), new FilenameFilter(){
                        @Override
                        public boolean accept( final File dir, final String name ) {
                            return name != null && pattern.matcher( name ).find();
                        }
                    }, processSubdirectoriesCheckBox.isSelected() );

                    // ensure interrupted status is cleared
                    Thread.interrupted();

                    return null;
                }
            }, this.getWizard(), "Finding Resources", "Finding resources, please wait ...", 2000 );
        } catch ( InterruptedException e ) {
            logger.log( Level.FINE, "Resource file search cancelled.", ExceptionUtils.getDebugException(e) );
        } catch ( InvocationTargetException e ) {
            logger.log( Level.WARNING, "Unexpected error during file resource search.", e );
            final String message = "Search error:\n"+ ExceptionUtils.getMessage(e);
            JOptionPane.showMessageDialog(this,
                                          Utilities.getTextDisplayComponent(message, 600, 100, -1, -1),
                                          "Error Finding Resources",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doSelect() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser( final JFileChooser fc ) {
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileFilter( new FileFilter(){
                    @Override
                    public String getDescription() {
                        return "Directory";
                    }

                    @Override
                    public boolean accept( final File file ) {
                        return true;
                    }
                } );

                if (JFileChooser.APPROVE_OPTION != fc.showDialog( GlobalResourceImportSearchStep.this, "Select")) {
                    return;
                }

                directoryTextField.setText( fc.getSelectedFile().getAbsolutePath() );
                directoryTextField.setCaretPosition( 0 );
            }
        });
    }

    private void doAddFromUrl() {
        final OkCancelDialog dlg = new OkCancelDialog<String>(this.getOwner(), "Add Resource From URL", true, new UrlPanel("Enter the URL for the resource", null));
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                String url = (String)dlg.getValue();
                if (url != null) {
                    try {
                        resourceInputTableModel.addRow( getContext().newResourceInputSource( new URI(url), (ResourceType)null) );
                    } catch ( URISyntaxException e ) {
                        showErrorMessage( GlobalResourceImportSearchStep.this, "Add From URL Error", "Error processing URL:\n"+url+"\n"+ExceptionUtils.getMessage(e));
                    }
                }
            }
        });
    }

    private void doAddFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser( final JFileChooser fc ) {
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setAcceptAllFileFilterUsed(true);

                if ( JFileChooser.APPROVE_OPTION != fc.showDialog( GlobalResourceImportSearchStep.this, "Add") ) {
                    return;
                }

                final File resourceFile = fc.getSelectedFile();
                if (resourceFile != null) {
                    resourceInputTableModel.addRow( getContext().newResourceInputSource(resourceFile) );
                }
            }
        });
    }

    private void doRemove() {
        final int[] rows = resourcesTable.getSelectedRows();
        if ( rows != null ) {

            final java.util.List<ResourceInputSource> inputSources = new ArrayList<ResourceInputSource>();
            for ( final int row : rows ) {
                final int modelRow = resourcesTable.convertRowIndexToModel( row );
                inputSources.add( resourceInputTableModel.getRowObject( modelRow ) );
            }

            for ( final ResourceInputSource inputSource : inputSources ) {
                resourceInputTableModel.removeRow( inputSource );
            }
        }
    }

    private void doClear() {
        resourceInputTableModel.setRows( new ArrayList<ResourceInputSource>() );
    }

    private void findSchemas( final File directory,
                              final FilenameFilter filter,
                              final boolean processSubdirectories ) throws InterruptedException {
        if ( Thread.interrupted() ) throw new InterruptedException();

        if ( directory.isDirectory() ) {
            final File[] files = directory.listFiles( filter );
            if ( files != null ) {
                for ( final File file : files ) {
                    final ResourceInputSource resourceInfo = getContext().newResourceInputSource( file );
                    if ( resourceInputTableModel.getRowIndex( resourceInfo ) == -1 ) {
                        resourceInputTableModel.addRow( resourceInfo );
                    }
                }

                if ( processSubdirectories ) {
                    final File[] directories = directory.listFiles( new FilenameFilter(){
                        @Override
                        public boolean accept( final File dir, final String name ) {
                            return new File(dir, name).isDirectory();
                        }
                    } );

                    if ( directories != null ) {
                        for ( final File dir : directories ) {
                            try {
                                final File canonicalDir = dir.getCanonicalFile();
                                if ( directory.equals( canonicalDir.getParentFile() ) ) {
                                    findSchemas( dir, filter, true );
                                }
                            } catch ( IOException e ) {
                                logger.log( Level.WARNING, "Error processing directory '"+dir.getAbsolutePath()+"'", e );
                            }
                        }
                    }
                }
            }
        } else if ( directory.exists() && filter.accept( directory.getParentFile(), directory.getName() )) {
            resourceInputTableModel.addRow( getContext().newResourceInputSource( directory ) );
        }
    }

    private void enableAndDisableComponents() {
        removeButton.setEnabled( resourcesTable.getSelectedRowCount() > 0 );
        clearButton.setEnabled( resourceInputTableModel.getRowCount() > 0 );
        findButton.setEnabled( directoryTextField.getText() != null && !directoryTextField.getText().trim().isEmpty() );
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canAdvance() {
        return resourcesTable.getRowCount() > 0;
    }

    @Override
    public void storeSettings( final GlobalResourceImportContext settings ) {
        settings.setResourceInputSources( resourceInputTableModel.getRows() );
    }

    @Override
    public void readSettings( final GlobalResourceImportContext settings ) {
        resourceInputTableModel.setRows( settings.getResourceInputSources() );
    }
}
