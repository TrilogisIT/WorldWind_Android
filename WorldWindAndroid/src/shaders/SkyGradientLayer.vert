/*
 * Input vertex attribute defining the surface vertex point in model coordinates. This attribute is specified in
 * TODO.
 */
attribute vec4 vertexPoint;
/*
 * Input vertex attribute defining the unique RGB color of each primitive. This attribute is specified in TODO. Although
 * this attribute can vary per-vertex, it is assumed that either (a) this is a constant value for the entire primitive,
 * or (b) the same color is assigned to each triangle vertex.
 */
attribute vec4 vertexColor;
/*
 * Input uniform matrix defining the current modelview-projection transform matrix. Maps model coordinates to eye
 * coordinates.
 */
uniform mat4 mvpMatrix;

/*
 * Output variable vector to TiledTessellatorPick.frag defining the color for each primitive (triangle). This is
 * specified for each vertex and is interpolated for each rasterized fragment of each primitive.
 */
varying vec4 primColor;

/*
 * OpenGL ES vertex shader entry point. Called for each vertex processed when this shader's program is bound.
 */
void main()
{
    /* Transform the surface vertex point from model coordinates to eye coordinates. */
    gl_Position = mvpMatrix * vertexPoint;

    /* Assign the varying fragment color to the current vertex's color. */
    primColor = vertexColor;
}
