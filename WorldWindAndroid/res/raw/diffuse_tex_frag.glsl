precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D sTexture;

uniform lowp float uOpacity;

/*
 * OpenGL ES fragment shader entry point. Called for each fragment rasterized when this shader's program is bound.
 */
void main()
{
    gl_FragColor = texture2D(sTexture, vTextureCoord);
    gl_FragColor = vec4(gl_FragColor.rgb, gl_FragColor.a * uOpacity);
}
