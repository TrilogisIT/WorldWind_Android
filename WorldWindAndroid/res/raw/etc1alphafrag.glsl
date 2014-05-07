precision mediump float;

varying vec2 vTextureCoord;

uniform sampler2D sTexture;
uniform sampler2D aTexture;

uniform lowp float uOpacity;

/*
 * Read alpha component from 2nd etc1 texture
 */
void main()
{
    gl_FragColor = vec4(texture2D(sTexture, vTextureCoord).rgb, texture2D(aTexture, vTextureCoord).r * uOpacity);
}
