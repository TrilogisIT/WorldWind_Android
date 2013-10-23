precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D sTexture;

/*
 * OpenGL ES fragment shader entry point. Called for each fragment rasterized when this shader's program is bound.
 */
void main()
{
    gl_FragColor = texture2D(sTexture, vTextureCoord);
    /*gl_FragColor = vec4(1,0,0,1);*/
}
