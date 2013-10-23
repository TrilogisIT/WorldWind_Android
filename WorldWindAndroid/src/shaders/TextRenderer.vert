uniform vec4 uTextureColor;
/*
 * Input vertex attribute defining the surface vertex point in model coordinates. This attribute is specified in
 * TODO.
 */
attribute vec4 vertexPoint;
attribute vec2 aTextureCoord;
/*
 * Input uniform matrix defining the current modelview-projection transform matrix. Maps model coordinates to eye
 * coordinates.
 */
uniform mat4 mvpMatrix;
varying vec2 vTextureCoord;
varying vec4 vTextureColor;

/*
 * OpenGL ES vertex shader entry point. Called for each vertex processed when this shader's program is bound.
 */
void main()
{
    /* Transform the surface vertex point from model coordinates to eye coordinates. */
    gl_Position = mvpMatrix * vertexPoint;
    vTextureCoord = aTextureCoord;
    vTextureColor = uTextureColor;
}
