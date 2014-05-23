/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.Logging;


/**
 * @author tag
 * @version $Id: Material.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Material
{
    private final Color ambient;
    private final Color diffuse;
    private final Color specular;
    private final Color emission;
    private final double shininess;

    public static final Material WHITE = new Material(Color.white());
    public static final Material LIGHT_GRAY = new Material(Color.lightGray());
    public static final Material GRAY = new Material(Color.gray());
    public static final Material DARK_GRAY = new Material(Color.darkGray());
    public static final Material BLACK = new Material(Color.black());
    public static final Material RED = new Material(Color.red());
    public static final Material PINK = new Material(Color.pink());
    public static final Material ORANGE = new Material(Color.orange());
    public static final Material YELLOW = new Material(Color.yellow());
    public static final Material GREEN = new Material(Color.green());
    public static final Material MAGENTA = new Material(Color.magenta());
    public static final Material CYAN = new Material(Color.cyan());
    public static final Material BLUE = new Material(Color.blue());

    public Material(Color specular, Color diffuse, Color ambient, Color emission, float shininess)
    {
        if (specular == null || diffuse == null || ambient == null || emission == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.ambient = ambient;
        this.diffuse = diffuse;
        this.specular = specular;
        this.emission = emission;
        this.shininess = shininess;
    }

    public Material(Color color, float shininess)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.ambient = this.makeDarker(color);
        this.diffuse = color;
        this.specular = new Color(255, 255, 255, color.a);
        this.emission = new Color(0, 0, 0, color.a);
        this.shininess = shininess;
    }

	public Material(Material copy) {
		if (copy == null)
		{
			String msg = Logging.getMessage("nullValue.MaterialIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.ambient = new Color(copy.ambient);
		this.diffuse = new Color(copy.diffuse);
		this.specular = new Color(copy.specular);
		this.emission = new Color(copy.emission);
		this.shininess = copy.shininess;
	}

    public Material(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.ambient = this.makeDarker(color);
        this.diffuse = color;
        this.specular = new Color(255, 255, 255, color.a);
        this.emission = new Color(0, 0, 0, color.a);
        this.shininess = 80.0f;
    }

    public final Color getAmbient()
    {
        return this.ambient;
    }

    public final Color getDiffuse()
    {
        return this.diffuse;
    }

    public final Color getSpecular()
    {
        return this.specular;
    }

    public final Color getEmission()
    {
        return this.emission;
    }

    public final double getShininess()
    {
        return this.shininess;
    }

    public void apply(DrawContext dc)
    {
		dc.getCurrentProgram().loadUniformColor("uColor", diffuse);
    }

    public void apply(DrawContext dc, float alpha)
    {
		dc.getCurrentProgram().loadUniformColor("uColor", new Color(diffuse.r, diffuse.g, diffuse.b, diffuse.a*alpha));
    }

    //protected void glMaterialPremult(GL2 gl, int face, int name, Color color)
    //{
    //    float[] compArray = new float[4];
    //    color.getRGBComponents(compArray);
    //    compArray[0] = compArray[0] * compArray[3];
    //    compArray[1] = compArray[1] * compArray[3];
    //    compArray[2] = compArray[2] * compArray[3];
    //    gl.glMaterialfv(face, name, compArray, 0);
    //}

    //protected void glMaterialfvPremult(GL2 gl, int face, int name, Color color, float alpha)
    //{
    //    float[] compArray = new float[4];
    //    color.getRGBColorComponents(compArray);
    //    compArray[0] = compArray[0] * alpha;
    //    compArray[1] = compArray[1] * alpha;
    //    compArray[2] = compArray[2] * alpha;
    //    compArray[3] = alpha;
    //    gl.glMaterialfv(face, name, compArray, 0);
    //}

    protected Color makeDarker(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        float factor = 0.3f;

        return new Color(
            Math.max(0, (int) (color.r * factor)),
            Math.max(0, (int) (color.g * factor)),
            Math.max(0, (int) (color.b * factor)),
            color.a);
    }

//    public void getRestorableState(RestorableSupport rs, RestorableSupport.StateObject so)
//    {
//        String encodedColor = RestorableSupport.encodeColor(this.ambient);
//        if (encodedColor != null)
//            rs.addStateValueAsString(so, "ambient", encodedColor);
//
//        encodedColor = RestorableSupport.encodeColor(this.diffuse);
//        if (encodedColor != null)
//            rs.addStateValueAsString(so, "diffuse", encodedColor);
//
//        encodedColor = RestorableSupport.encodeColor(this.specular);
//        if (encodedColor != null)
//            rs.addStateValueAsString(so, "specular", encodedColor);
//
//        encodedColor = RestorableSupport.encodeColor(this.emission);
//        if (encodedColor != null)
//            rs.addStateValueAsString(so, "emission", encodedColor);
//
//        rs.addStateValueAsDouble(so, "shininess", this.shininess);
//    }
//
//    public Material restoreState(RestorableSupport rs, RestorableSupport.StateObject so)
//    {
//        double shininess = this.getShininess();
//        Double d = rs.getStateValueAsDouble(so, "shininess");
//        if (d != null)
//            shininess = d;
//
//        String as = rs.getStateValueAsString(so, "ambient");
//        Color ambient = RestorableSupport.decodeColor(as);
//        if (ambient == null)
//            ambient = this.getAmbient();
//
//        String ds = rs.getStateValueAsString(so, "diffuse");
//        Color diffuse = RestorableSupport.decodeColor(ds);
//        if (diffuse == null)
//            diffuse = this.getDiffuse();
//
//        String ss = rs.getStateValueAsString(so, "specular");
//        Color specular = RestorableSupport.decodeColor(ss);
//        if (specular == null)
//            specular = this.getSpecular();
//
//        String es = rs.getStateValueAsString(so, "emission");
//        Color emission = RestorableSupport.decodeColor(es);
//        if (emission == null)
//            emission = this.getEmission();
//
//        return new Material(specular, diffuse, ambient, emission, (float) shininess);
//    }

    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        Material that = (Material) o;

        if (Double.compare(this.shininess, that.shininess) != 0)
            return false;
        if (this.ambient != null ? !this.ambient.equals(that.ambient) : that.ambient != null)
            return false;
        if (this.diffuse != null ? !this.diffuse.equals(that.diffuse) : that.diffuse != null)
            return false;
        if (this.specular != null ? !this.specular.equals(that.specular) : that.specular != null)
            return false;
        //noinspection RedundantIfStatement
        if (this.emission != null ? !this.emission.equals(that.emission) : that.emission != null)
            return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        long temp = (this.shininess != +0.0d) ? Double.doubleToLongBits(this.shininess) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (this.ambient != null ? this.ambient.hashCode() : 0);
        result = 31 * result + (this.diffuse != null ? this.diffuse.hashCode() : 0);
        result = 31 * result + (this.specular != null ? this.specular.hashCode() : 0);
        result = 31 * result + (this.emission != null ? this.emission.hashCode() : 0);
        return result;
    }
}
