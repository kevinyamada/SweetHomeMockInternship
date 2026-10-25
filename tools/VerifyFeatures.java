import com.eteks.sweethome3d.io.*;
import com.eteks.sweethome3d.model.*;
import com.eteks.sweethome3d.swing.*;
import com.eteks.sweethome3d.viewcontroller.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableColumnModel;

/** Standalone integration checks. Run on a graphical desktop after mvn compile. */
public class VerifyFeatures {
  private static void require(boolean ok, String message) {
    if (!ok) throw new AssertionError(message);
  }
  private static void render(Component component, String path) throws IOException {
    BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try { component.paintAll(graphics); } finally { graphics.dispose(); }
    ImageIO.write(image, "png", new File(path));
  }
  private static boolean hasVolume(MenuElement menu) {
    if (menu instanceof JMenuItem && "Volume".equals(((JMenuItem)menu).getText())) return true;
    for (MenuElement child : menu.getSubElements()) if (hasVolume(child)) return true;
    return false;
  }
  public static void main(String[] args) throws Exception {
    System.setProperty("com.eteks.sweethome3d.no3D", "true");
    Locale.setDefault(Locale.ENGLISH);
    SwingUtilities.invokeAndWait(() -> {
      try { verify(); } catch (Exception e) { throw new RuntimeException(e); }
    });
    System.out.println("Integration checks passed; evidence rendered from the application components.");
    System.exit(0);
  }
  private static void verify() throws Exception {
    DefaultUserPreferences prefs = new DefaultUserPreferences() {
      @Override public void write() { }
    };
    prefs.setLanguage("en");
    SwingViewFactory factory = new SwingViewFactory();
    Home home = new HomeFileRecorder().readHome("samples/MultiLevelHome.sh3d");
    home.setName("MultiLevelHome.sh3d");
    HomeController controller = new HomeController(home, prefs, factory, new FileContentManager(prefs));
    HomePane pane = (HomePane)controller.getView();
    JFrame frame = new JFrame("Sweet Home 3D - MultiLevelHome");
    frame.setContentPane(pane);
    frame.setSize(1360, 900);
    frame.setVisible(true);
    frame.validate();
    render(frame, "submissions/task1-running.png");

    HomePieceOfFurniture piece = home.getFurniture().get(0);
    home.setSelectedItems(Collections.singletonList(piece));
    HomeFurnitureController edit = new HomeFurnitureController(home, prefs, factory, new FileContentManager(prefs), null);
    edit.setDescription("Edited furniture description - verified persistence");
    edit.modifyFurniture();
    require(piece.getDescription().equals(edit.getDescription()), "Description must be stored on furniture");
    JPanel panel = (JPanel)edit.getView();
    JDialog dialog = new JDialog(frame, "Modify furniture", false);
    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setVisible(true);
    render(dialog, "submissions/task2-description.png");
    dialog.dispose();

    CatalogPieceOfFurniture catalog = new CatalogPieceOfFurniture("Volume check", null, null, 2, 3, 4, true, false);
    require(catalog.getVolume() == 24, "Catalog volume");
    HomePieceOfFurniture model = new HomePieceOfFurniture(catalog);
    model.setWidth(5);
    require(model.getVolume() == 60, "Volume must update after resizing");
    FurnitureController furniture = controller.getFurnitureController();
    if (!home.getFurnitureVisibleProperties().contains(HomePieceOfFurniture.SortableProperty.VOLUME))
      furniture.toggleFurnitureVisibleProperty(HomePieceOfFurniture.SortableProperty.VOLUME);
    furniture.toggleFurnitureSort(HomePieceOfFurniture.SortableProperty.VOLUME);
    home.setFurnitureDescendingSorted(false);
    FurnitureTable table = (FurnitureTable)furniture.getView();
    float previous = -1;
    for (int row=0; row<table.getRowCount(); row++) {
      HomePieceOfFurniture rowPiece = (HomePieceOfFurniture)table.getValueAt(row, 0);
      if (!home.getFurniture().contains(rowPiece)) continue;
      float volume = rowPiece.getVolume();
      require(volume >= previous, "Ascending volume order"); previous = volume;
    }
    home.setFurnitureDescendingSorted(true);
    previous = Float.POSITIVE_INFINITY;
    for (int row=0; row<table.getRowCount(); row++) {
      HomePieceOfFurniture rowPiece = (HomePieceOfFurniture)table.getValueAt(row, 0);
      if (!home.getFurniture().contains(rowPiece)) continue;
      float volume = rowPiece.getVolume();
      require(volume <= previous, "Descending volume order"); previous = volume;
    }
    home.setFurnitureDescendingSorted(false);
    require(hasVolume(pane.getJMenuBar()), "Main menu Volume");
    require(hasVolume(table.getComponentPopupMenu()), "Context menu Volume");
    frame.validate();
    render(frame, "submissions/task3-volume.png");
    // Render the real menu separately so its entries remain readable.
    for (int i=0; i<pane.getJMenuBar().getMenuCount(); i++) {
      JMenu menu = pane.getJMenuBar().getMenu(i);
      if (menu != null && "Furniture".equals(menu.getText())) {
        for (MenuElement element : menu.getPopupMenu().getSubElements()) {
          if (element instanceof JMenu && hasVolume(element)) {
            JPopupMenu popup = ((JMenu)element).getPopupMenu();
            popup.setSize(popup.getPreferredSize());
            popup.doLayout();
            BufferedImage image = new BufferedImage(popup.getWidth(), popup.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics(); popup.printAll(g); g.dispose();
            ImageIO.write(image, "png", new File("submissions/task3-volume-menu.png"));
            break;
          }
        }
      }
    }

    home.setSelectedLevel(home.getLevels().get(home.getLevels().size()-1));
    Level original = home.getSelectedLevel();
    java.util.List<HomePieceOfFurniture.SortableProperty> columns = new ArrayList<>(home.getFurnitureVisibleProperties());
    TableColumnModel columnModel = table.getColumnModel();
    FurnitureTable.FurnitureFilter filter = (h, p) -> p.isAtLevel(original);
    table.setFurnitureFilter(filter);
    home.setPrint(new HomePrint(HomePrint.PaperOrientation.PORTRAIT, 612, 792, 36, 36, 36, 36,
        true, true, false, null, null, null));
    HomePrintableComponent printable = new HomePrintableComponent(home, controller, new Font("SansSerif", Font.PLAIN, 10));
    require(printable.getPageCount() == 4, "Sample must print one furniture page plus three plans");
    try (OutputStream out = new FileOutputStream("submissions/task4-all-levels.pdf")) {
      new HomePDFPrinter(home, prefs, controller, new Font("SansSerif", Font.PLAIN, 10)).write(out);
    }
    require(home.getSelectedLevel() == original, "Print restores selected level");
    require(home.getFurnitureVisibleProperties().equals(columns), "Print preserves visible properties");
    require(table.getColumnModel() == columnModel, "Print restores screen columns");
    require(table.getFurnitureFilter() == filter, "Print restores screen filter");
    checkPlanEdgeCases(prefs);
    frame.dispose();
  }
  private static void checkPlanEdgeCases(DefaultUserPreferences prefs) {
    Home home = new Home();
    Level visible = new Level("Visible", 0, 10, 240);
    Level hidden = new Level("Hidden", 250, 10, 240);
    Level empty = new Level("Empty", 500, 10, 240);
    home.addLevel(visible); home.addLevel(hidden); home.addLevel(empty);
    hidden.setViewable(false);
    home.setSelectedLevel(hidden);
    PlanComponent plan = new PlanComponent(home, prefs, null);
    BufferedImage image = new BufferedImage(800,1000,BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    PageFormat format = new PageFormat();
    try {
      require(plan.print(g,format,0) == Printable.PAGE_EXISTS, "Empty visible level has a page");
      require(plan.print(g,format,1) == Printable.PAGE_EXISTS, "Second viewable level has a page");
      require(plan.print(g,format,2) == Printable.NO_SUCH_PAGE, "Hidden level is skipped");
      require(plan.print(g,format,-1) == Printable.NO_SUCH_PAGE, "Negative page rejected");
      require(home.getSelectedLevel() == hidden, "Hidden selection restored");
      Wall wall = new Wall(0,0,200,0,10,240); home.addWall(wall); wall.setLevel(visible);
      PlanComponent failing = new PlanComponent(home,prefs,null) {
        @Override public float getPrintPreferredScale(Graphics graphics, PageFormat page) {
          throw new IllegalStateException("Deliberate render failure");
        }
      };
      try { failing.print(g,format,0); throw new AssertionError("Expected render failure"); }
      catch (IllegalStateException expected) { require(home.getSelectedLevel() == hidden, "Selection restored after failure"); }
    } finally { g.dispose(); }
  }
}
