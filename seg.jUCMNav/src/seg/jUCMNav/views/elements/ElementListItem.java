package seg.jUCMNav.views.elements;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import seg.jUCMNav.figures.ColorManager;
import seg.jUCMNav.views.compositeList.CompositeListControl;
import seg.jUCMNav.views.compositeList.CompositeListItem;

/**
 * A CompositeListItem representing a responsibility reference, or and IntentionalElementRef, or and KPIInformationElementRef used in the Elements view.
 * 
 * @author Etienne Tremblay, pchen
 * 
 */
public class ElementListItem extends CompositeListItem {

    private static final String DESC_FONT_KEY = "seg.jUCMNav.views.elements.ElementListItem.descFont"; //$NON-NLS-1$
    private static final String NAME_FONT_KEY = "seg.jUCMNav.views.elements.ElementListItem.nameFont"; //$NON-NLS-1$

    private Label lblDesc = null;

    private Composite composite = null;
    private Label lblIcon = null;
    private Label lblName = null;

    // Tracked so the per-item Image allocated for the icon is disposed when the widget itself is disposed.
    private Image currentIconImage = null;

    private static Font getCachedFont(String key, int style) {
        if (!JFaceResources.getFontRegistry().hasValueFor(key)) {
            JFaceResources.getFontRegistry().put(key, new FontData[] { new FontData("Tahoma", 8, style) }); //$NON-NLS-1$
        }
        return JFaceResources.getFontRegistry().get(key);
    }

    private void replaceIconImage(Image next) {
        if (currentIconImage != null && !currentIconImage.isDisposed())
            currentIconImage.dispose();
        currentIconImage = next;
        lblIcon.setImage(next);
    }

    /**
     * @param parent
     * @param style
     */
    public ElementListItem(Composite parent, int style) {
        super(parent, style);
        if (parent instanceof CompositeListControl) {
            CompositeListControl c = (CompositeListControl) parent;
            c.add(this);
        }
        initialize();
    }

    private void initialize() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.makeColumnsEqualWidth = false;
        this.setLayout(layout);
        createComposite();
        final GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
        data.verticalSpan = 2;
        lblDesc = new Label(this, SWT.WRAP);
        lblDesc.setLayoutData(data);
        lblDesc.setText(""); //$NON-NLS-1$
        lblDesc.setFont(getCachedFont(DESC_FONT_KEY, SWT.ITALIC));
        lblDesc.setBackground(org.eclipse.swt.widgets.Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));
        this.setBackground(org.eclipse.swt.widgets.Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));
        layout.marginWidth = 3;
        layout.marginHeight = 3;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        setSize(new org.eclipse.swt.graphics.Point(270, 52));
        lblDesc.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
                fireSelected(e);
            }
        });

    }

    public void select() {
        super.select();
        this.setBackground(ColorManager.BLACK);
        lblDesc.setBackground(ColorManager.LIGHTBLUE);
        lblName.setBackground(ColorManager.LIGHTBLUE);
        composite.setBackground(ColorManager.LIGHTBLUE);
        lblIcon.setBackground(ColorManager.LIGHTBLUE);
    }

    public void unselect() {
        super.unselect();
        this.setBackground(ColorManager.WHITE);
        lblDesc.setBackground(ColorManager.WHITE);
        lblName.setBackground(ColorManager.WHITE);
        composite.setBackground(ColorManager.WHITE);
        lblIcon.setBackground(ColorManager.WHITE);
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return lblDesc.getText();
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description) {
        this.lblDesc.setText(description);
    }

    /**
     * @return Returns the name.
     */
    public String getElementName() {
        return lblName.getText();
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setElementName(String name) {
        this.lblName.setText(name);
    }

    /**
     * @param path
     *            The image to set. Default image is the responsibility image
     */
    public void setElementImg(String path) {
        InputStream stream = ElementListItem.class.getResourceAsStream(path);
        replaceIconImage(new Image(Display.getCurrent(), stream));
        lblIcon.setBackground(ColorManager.WHITE);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method initializes composite
     * 
     */
    private void createComposite() {
        GridData gridData3 = new GridData();
        GridLayout gridLayout2 = new GridLayout();
        GridData gridData1 = new GridData();
        composite = new Composite(this, SWT.NONE);
        lblIcon = new Label(composite, SWT.NONE);
        lblName = new Label(composite, SWT.NONE);
        lblName.setBackground(org.eclipse.swt.widgets.Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));
        lblIcon.setText(""); //$NON-NLS-1$
        InputStream stream = getClass().getResourceAsStream("/seg/jUCMNav/icons/Resp16.gif");//$NON-NLS-1$
        replaceIconImage(new Image(Display.getCurrent(), stream));
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                if (currentIconImage != null && !currentIconImage.isDisposed())
                    currentIconImage.dispose();
            }
        });

        lblName.setText(""); //$NON-NLS-1$
        lblName.setLayoutData(gridData3);
        lblName.setFont(getCachedFont(NAME_FONT_KEY, SWT.BOLD));
        composite.setBackground(org.eclipse.swt.widgets.Display.getDefault().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));
        composite.setLayoutData(gridData1);
        composite.setLayout(gridLayout2);
        gridData1.grabExcessHorizontalSpace = true;
        gridData1.grabExcessVerticalSpace = true;
        gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData1.verticalSpan = 2;
        gridLayout2.numColumns = 2;
        gridLayout2.horizontalSpacing = 3;
        gridLayout2.marginHeight = 0;
        gridLayout2.marginWidth = 0;
        gridLayout2.verticalSpacing = 0;
        gridData3.grabExcessHorizontalSpace = true;
        gridData3.grabExcessVerticalSpace = true;
        gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
        gridData3.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
        lblName.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
                fireSelected(e);
            }
        });
        lblIcon.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {
            public void mouseDown(org.eclipse.swt.events.MouseEvent e) {
                fireSelected(e);
            }
        });
    }
} // @jve:decl-index=0:visual-constraint="10,10"
