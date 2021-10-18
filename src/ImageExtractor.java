
import java.io.IOException;
import java.util.LinkedList;
import java.util.Stack;

public class ImageExtractor extends Main {
  private static boolean[] screen, background;
  private static int[] pixels;
  private static int x1, y1, x2, y2, imageNumber;
  private static final int SAME = 0, CHANGED = 1;
  
  private static class Coords {
    private int x, y;

    public Coords(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }
  
  private static Stack<Coords> coordsStack = new Stack<>();
  
  public static final LinkedList<LinkedList<Image>> images = new LinkedList<>();
  
  public static void extract(boolean[] scr, boolean[] backgr) {
    screen = scr;
    background = backgr;
    imageNumber = 1;
    
    pixels = new int[PIXEL_SIZE];
    for(int addr = 0; addr < PIXEL_SIZE; addr++)
      pixels[addr] = screen[addr] == background[addr] ? SAME : CHANGED;
    
    
    for(int y = 0; y < PIXEL_HEIGHT; y++) {
      int ySource = y << 8;
      x: for(int x = 0; x < PIXEL_WIDTH; x++) {
        int addr = ySource | x;
        if(pixels[addr] == CHANGED) {
          x1 = x2 = x;
          y1 = y2 = y;
          imageNumber++;
          pixels[addr] = imageNumber;
          coordsStack.add(new Coords(x, y));
          while(!coordsStack.empty()) {
            Coords coords = coordsStack.pop();
            int x0 = coords.x;
            int y0 = coords.y;
            for(int dy = -MAX_DISTANCE; dy <= MAX_DISTANCE; dy++) {
              int yy = y0 + dy;
              int yAddr = yy * PIXEL_WIDTH;
              if(yy < 0 || yy >= PIXEL_HEIGHT) continue;
              for(int dx = -MAX_DISTANCE; dx <= MAX_DISTANCE; dx++) {
                if(dx == 0 && dy == 0) continue;
                int xx = x0 + dx;
                if(xx < 0 || xx >= PIXEL_WIDTH) continue;
                int addr2 = x0 + dx + yAddr;
                if(pixels[addr2] == CHANGED) {
                  x1 = Integer.min(x1, xx);
                  y1 = Integer.min(y1, yy);
                  x2 = Integer.max(x2, xx);
                  y2 = Integer.max(y2, yy);
                  pixels[addr2] = imageNumber;
                  coordsStack.add(new Coords(xx, yy));
                }
              }
            }
          } 
          if(!Image.hasAcceptableSize(x1, y1, x2, y2)) continue;
          Image image = new Image(pixels, screen, background, x1, y1, x2, y2
              , imageNumber);
          for(LinkedList<Image> list: images) {
            for(Image listImage: list) {
              switch(listImage.compareTo(image)) {
                case EQUAL:
                  continue x;
                case SIMILAR:
                  list.add(image);
                  continue x;
                case OTHER:
                default:
                  break;
              }
            }
          }

          LinkedList<Image> newList = new LinkedList<>();
          newList.add(image);
          images.add(newList);
        }
      }
    }
  }

  public static void saveImages() throws IOException {
    for(LinkedList<Image> list: images) {
      int maxWeight = -1;
      Image maxImage = null;
      for(Image image: list) {
        int size = image.getWeight();
        if(maxWeight < size) {
          maxWeight = size;
          maxImage = image;
        }
      }
      maxImage.save();
    }
  }
}