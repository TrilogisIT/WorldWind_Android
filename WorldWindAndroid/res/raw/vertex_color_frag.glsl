precision mediump float;

/*
 * Input varying vector from TiledTessellatorPick.vert defining the color for each primitive (triangle). This is
 * specified for each vertex and is interpolated for each rasterized fragment of each primitive.
 */
varying vec4 primColor;

uniform lowp float uOpacity;

uniform vec4 uColor;

uniform bool uUseVertexColor;

/*
 * OpenGL ES fragment shader entry point. Called for each fragment rasterized when this shader's program is bound.
 */
void main()
{
    /* Assign the fragment color to the varying vertex color or uniform color. */
    if(uUseVertexColor)
        gl_FragColor = primColor;
    else
        gl_FragColor = uColor;
    gl_FragColor = vec4(gl_FragColor.rgb, gl_FragColor.a * uOpacity);
}
