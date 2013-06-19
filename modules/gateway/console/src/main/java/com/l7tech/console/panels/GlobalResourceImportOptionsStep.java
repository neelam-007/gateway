package com.l7tech.console.panels;

import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.console.panels.GlobalResourceImportContext.ImportOption;
import static com.l7tech.console.panels.GlobalResourceImportContext.ImportChoice;

/**
 * Global resource import wizard options step.
 */
class GlobalResourceImportOptionsStep extends GlobalResourceImportWizardStepPanel {

    private JPanel mainPanel;
    private JPanel optionsPanel;

    private final Map<ImportOption, JComboBox> optionCombos = new HashMap<ImportOption,JComboBox>();
    private Map<ImportOption,ImportChoice> importOptions = Collections.emptyMap();
    private Collection<GlobalResourceImportContext.ResourceInputSource> inputSources = Collections.emptySet();
    private Map<String, GlobalResourceImportContext.ResourceHolder> processedResources = Collections.emptyMap();
    private SecurityZoneWidget zoneControl;

    GlobalResourceImportOptionsStep( final GlobalResourceImportWizardStepPanel next ) {
        super( "options-step", next );
        init();
    }

    private void init() {
        setLayout( new BorderLayout() );
        add(mainPanel, BorderLayout.CENTER);

        optionsPanel.setLayout( new BoxLayout( optionsPanel, BoxLayout.Y_AXIS ) );
        optionsPanel.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ));

        for ( final ImportOption option : ImportOption.values() ) {
            final JLabel optionLabel = new JLabel( resources.getString( "option." + option.name() ));
            setBoxOptions( optionLabel );
            optionsPanel.add( optionLabel );

            final JComboBox optionComboBox = new JComboBox();
            final EnumSet<ImportChoice> choices = EnumSet.of( ImportChoice.NONE );
            choices.addAll( option.getImportChoices() );
            optionComboBox.setModel( new DefaultComboBoxModel( choices.toArray() ) );
            optionComboBox.setRenderer( new TextListCellRenderer<ImportChoice>( new Functions.Unary<String,ImportChoice>(){
                @Override
                public String call( final ImportChoice importChoice ) {
                    String text;
                    final String optionChoiceKey = "option." + option.name() + "." + importChoice.name();
                    if ( resources.containsKey( optionChoiceKey ) ) {
                        text = resources.getString( optionChoiceKey );
                    } else {
                        text = resources.getString( "choice." + importChoice.name() );
                    }
                    return text;
                }
            } ) );
            setBoxOptions( optionComboBox );

            optionCombos.put( option, optionComboBox );
            optionsPanel.add( Box.createVerticalStrut( 4 ) );
            optionsPanel.add( optionComboBox );
            optionsPanel.add( Box.createVerticalStrut( 12 ) );
        }
        
        zoneControl = new SecurityZoneWidget();
        setBoxOptions(zoneControl);
        optionsPanel.add(zoneControl);
        optionsPanel.add( Box.createVerticalGlue() );
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canAdvance() {
        return true;
    }

    @Override
    public boolean onNextButton() {
        // process resources
        getContext().setImportOptions( buildImportOptions() );
        getContext().setSecurityZone(zoneControl.getSelectedZone());
        processedResources = GlobalResourceImportWizard.processResources( getContext(), inputSources );
        return true;
    }

    @Override
    public void readSettings( final GlobalResourceImportContext settings ) {
        importOptions = settings.getImportOptions();
        inputSources = settings.getResourceInputSources();
        applyOptions();
        zoneControl.configure(EntityType.RESOURCE_ENTRY, OperationType.CREATE, settings.getSecurityZone());
    }

    @Override
    public void storeSettings( final GlobalResourceImportContext settings ) {
        settings.setProcessedResources( processedResources );
    }

    private void applyOptions() {
        for ( final Map.Entry<ImportOption,ImportChoice> optionEntry : importOptions.entrySet() ) {
            final JComboBox optionComboBox = optionCombos.get( optionEntry.getKey() );
            if ( optionComboBox != null ) {
                optionComboBox.setSelectedItem( optionEntry.getValue() );
            }
        }
    }

    private Map<ImportOption,ImportChoice> buildImportOptions() {
        final Map<ImportOption,ImportChoice> importOptions = new HashMap<ImportOption,ImportChoice>();

        for ( final Map.Entry<ImportOption,JComboBox> optionAndCombo : optionCombos.entrySet() ) {
            if ( optionAndCombo.getValue().getSelectedItem() == null ) {
                importOptions.put( optionAndCombo.getKey(), ImportChoice.NONE );                
            } else {
                importOptions.put( optionAndCombo.getKey(), (ImportChoice)optionAndCombo.getValue().getSelectedItem() );
            }
        }

        return importOptions;
    }

    private void setBoxOptions( final JComponent component ) {
        component.setMaximumSize( new Dimension( Short.MAX_VALUE, component.getPreferredSize().height ) );
        component.setAlignmentX( Component.LEFT_ALIGNMENT );
    }
}
