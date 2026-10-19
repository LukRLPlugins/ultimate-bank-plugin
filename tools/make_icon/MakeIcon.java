import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Draws the Plugin Hub icon (48x72): an open chest whose inside is a starry void, with a vortex widening upwards
 * and items bigger than the chest floating out of it ("bigger on the inside").
 *
 * usage (JDK 11+): java tools/make_icon/MakeIcon.java icon.png [preview.png]
 */
public class MakeIcon
{
	private static final int W = 48;
	private static final int H = 72;
	/** Drawn at this scale, then smoothly downsampled */
	private static final int S = 8;

	public static void main(String[] args) throws Exception
	{
		BufferedImage big = new BufferedImage(W * S, H * S, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = big.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.scale(S, S);

		drawLid(g);
		drawVortex(g);
		drawItems(g);
		drawChest(g);
		g.dispose();

		BufferedImage icon = downsample(big, W, H);
		ImageIO.write(icon, "png", new File(args[0]));
		if (args.length > 1)
		{
			ImageIO.write(upscale(icon, 6), "png", new File(args[1]));
		}
	}

	/** A purple funnel rising from the chest mouth, much wider than the chest at the top */
	private static void drawVortex(Graphics2D g)
	{
		GeneralPath funnel = new GeneralPath();
		funnel.moveTo(18, 48);
		funnel.curveTo(15, 38, 2, 26, 1, 6);
		funnel.curveTo(14, 1, 34, 1, 47, 6);
		funnel.curveTo(46, 26, 33, 38, 30, 48);
		funnel.closePath();
		g.setPaint(new GradientPaint(0, 47, new Color(120, 60, 200, 230), 0, 2, new Color(70, 40, 160, 40)));
		g.fill(funnel);

		// Swirl lines
		g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < 4; i++)
		{
			float y = 44 - i * 10;
			float half = 7 + i * 6.5f;
			g.setColor(new Color(200, 170, 255, 170 - i * 30));
			g.draw(new java.awt.geom.Arc2D.Float(24 - half, y - 2.5f, half * 2, 5, 200, 250, java.awt.geom.Arc2D.OPEN));
		}

		// Stars
		Random random = new Random(7);
		for (int i = 0; i < 18; i++)
		{
			float y = 6 + random.nextFloat() * 38;
			float spread = 10 + (47 - y) * 0.45f;
			float x = 24 + (random.nextFloat() * 2 - 1) * spread * 0.85f;
			float r = random.nextFloat() < 0.25f ? 0.9f : 0.5f;
			g.setColor(new Color(255, 245, 200, 220));
			g.fill(new Ellipse2D.Float(x - r, y - r, r * 2, r * 2));
		}
	}

	/** Items larger than the chest itself, floating up out of the vortex */
	private static void drawItems(Graphics2D g)
	{
		// Sword, tilted
		AffineTransform saved = g.getTransform();
		g.translate(30, 24);
		g.rotate(Math.toRadians(28));
		g.setColor(new Color(215, 225, 235));
		g.fill(new Polygon(new int[]{-2, 2, 2, 0, -2}, new int[]{-16, -16, 8, 11, 8}, 5));
		g.setColor(new Color(150, 160, 175));
		g.fill(new java.awt.Rectangle(0, -16, 1, 24));
		g.setColor(new Color(212, 175, 55));
		g.fill(new RoundRectangle2D.Float(-6, -18, 12, 2.5f, 1, 1));
		g.setColor(new Color(110, 70, 40));
		g.fill(new java.awt.Rectangle(-1, -24, 2, 6));
		g.setColor(new Color(212, 175, 55));
		g.fill(new Ellipse2D.Float(-1.6f, -26, 3.2f, 3.2f));
		g.setTransform(saved);

		// Cape, flowing
		GeneralPath cape = new GeneralPath();
		cape.moveTo(9, 13);
		cape.lineTo(19, 13);
		cape.curveTo(21, 21, 21, 29, 22, 36);
		cape.curveTo(17, 34, 11, 37, 6, 34);
		cape.curveTo(8, 27, 8, 20, 9, 13);
		cape.closePath();
		g.setPaint(new GradientPaint(0, 13, new Color(200, 40, 40), 0, 36, new Color(120, 15, 25)));
		g.fill(cape);
		g.setColor(new Color(240, 200, 80));
		g.fill(new RoundRectangle2D.Float(8.5f, 12, 11, 2.5f, 1, 1));

		// Potion
		g.setColor(new Color(200, 230, 255, 200));
		g.fill(new RoundRectangle2D.Float(36.5f, 27, 3, 3, 1, 1));
		g.setPaint(new RadialGradientPaint(38, 33, 4.5f, new float[]{0, 1},
			new Color[]{new Color(120, 255, 160), new Color(20, 150, 70)}));
		g.fill(new Ellipse2D.Float(33.5f, 29, 9, 8.5f));
		g.setColor(new Color(140, 95, 60));
		g.fill(new java.awt.Rectangle(37, 25, 2, 2));
	}

	private static void drawLid(Graphics2D g)
	{
		Color woodDark = new Color(90, 52, 24);
		Color gold = new Color(222, 180, 60);
		Color outline = new Color(40, 22, 10);
		BasicStroke line = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		// Open lid, tipped back behind the mouth
		Polygon lid = new Polygon(new int[]{8, 40, 37, 11}, new int[]{46, 46, 39, 39}, 4);
		g.setColor(woodDark);
		g.fill(lid);
		g.setColor(outline);
		g.setStroke(line);
		g.draw(lid);
		g.setColor(gold);
		g.fill(new java.awt.Rectangle(11, 42, 26, 1));

	}

	private static void drawChest(Graphics2D g)
	{
		Color woodDark = new Color(90, 52, 24);
		Color wood = new Color(140, 85, 40);
		Color woodLight = new Color(175, 112, 55);
		Color gold = new Color(222, 180, 60);
		Color outline = new Color(40, 22, 10);
		BasicStroke line = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		// Mouth: a dark starry void
		Ellipse2D.Float mouth = new Ellipse2D.Float(7, 44, 34, 7);
		g.setPaint(new RadialGradientPaint(24, 47.5f, 17, new float[]{0, 0.6f, 1},
			new Color[]{new Color(10, 5, 30), new Color(55, 20, 110), new Color(150, 80, 230)}));
		g.fill(mouth);
		g.setComposite(AlphaComposite.SrcOver);
		g.setColor(new Color(255, 245, 200));
		g.fill(new Ellipse2D.Float(18, 46.5f, 1, 1));
		g.fill(new Ellipse2D.Float(28, 47.5f, 1, 1));
		g.fill(new Ellipse2D.Float(23, 49, 0.8f, 0.8f));

		// Body
		RoundRectangle2D.Float body = new RoundRectangle2D.Float(6, 48, 36, 21, 3, 3);
		g.setPaint(new GradientPaint(0, 48, woodLight, 0, 69, wood));
		g.fill(body);
		g.setColor(woodDark);
		g.fill(new java.awt.Rectangle(6, 55, 36, 1));
		g.fill(new java.awt.Rectangle(6, 62, 36, 1));
		// Gold bands and lock
		g.setColor(gold);
		g.fill(new java.awt.Rectangle(10, 48, 3, 21));
		g.fill(new java.awt.Rectangle(35, 48, 3, 21));
		g.fill(new RoundRectangle2D.Float(20.5f, 52, 7, 8, 1.5f, 1.5f));
		g.setColor(outline);
		g.fill(new Ellipse2D.Float(23.2f, 54.5f, 1.6f, 1.6f));
		g.fill(new java.awt.Rectangle(23, 55, 1, 3));
		g.setStroke(line);
		g.draw(body);
		g.setColor(new Color(190, 140, 255, 160));
		g.draw(mouth);
	}

	/** Box-filter downsample for smooth edges */
	private static BufferedImage downsample(BufferedImage src, int w, int h)
	{
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int fx = src.getWidth() / w;
		int fy = src.getHeight() / h;
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				long a = 0, r = 0, gr = 0, b = 0;
				for (int dy = 0; dy < fy; dy++)
				{
					for (int dx = 0; dx < fx; dx++)
					{
						int p = src.getRGB(x * fx + dx, y * fy + dy);
						int pa = p >>> 24;
						a += pa;
						r += ((p >> 16) & 0xff) * pa;
						gr += ((p >> 8) & 0xff) * pa;
						b += (p & 0xff) * pa;
					}
				}
				int n = fx * fy;
				int alpha = (int) (a / n);
				int rr = a == 0 ? 0 : (int) (r / a);
				int gg = a == 0 ? 0 : (int) (gr / a);
				int bb = a == 0 ? 0 : (int) (b / a);
				out.setRGB(x, y, (alpha << 24) | (rr << 16) | (gg << 8) | bb);
			}
		}
		return out;
	}

	private static BufferedImage upscale(BufferedImage src, int k)
	{
		BufferedImage out = new BufferedImage(src.getWidth() * k, src.getHeight() * k, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(40, 40, 40));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.drawImage(src, 0, 0, out.getWidth(), out.getHeight(), null);
		g.dispose();
		return out;
	}
}
