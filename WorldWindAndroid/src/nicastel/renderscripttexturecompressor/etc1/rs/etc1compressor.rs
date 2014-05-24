/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma version(1)
#pragma rs_fp_imprecise
#pragma rs java_package_name(nicastel.renderscripttexturecompressor.etc1.rs)


// DXT compressor copied here since I found no way to import properly
static inline uint8_t AV_RL16(const uint8_t * x) {
	return ((((const uint8_t*)(x))[1] << 8) |
			 ((const uint8_t*)(x))[0]);
}

static inline uint8_t AV_RL32(const uint8_t * x) {
	return ((((const uint8_t*)(x))[3] << 24) |
			(((const uint8_t*)(x))[2] << 16) |
			(((const uint8_t*)(x))[1] <<  8) |
			 ((const uint8_t*)(x))[0]);
}

static inline uint8_t AV_RL64(const uint8_t * x) {
	return     (((uint64_t)((const uint8_t*)(x))[7] << 56) |       
 	  ((uint64_t)((const uint8_t*)(x))[6] << 48) |       
      ((uint64_t)((const uint8_t*)(x))[5] << 40) |       
      ((uint64_t)((const uint8_t*)(x))[4] << 32) |       
      ((uint64_t)((const uint8_t*)(x))[3] << 24) |       
      ((uint64_t)((const uint8_t*)(x))[2] << 16) |       
      ((uint64_t)((const uint8_t*)(x))[1] <<  8) |       
       (uint64_t)((const uint8_t*)(x))[0]);
}                                        
                                                                     
static inline void dxt1_decode_pixels(const uint8_t *s, uint32_t *d,
                                       unsigned int qstride, unsigned int flag,uint64_t alpha) {             
     unsigned int x, y, c0, c1, a = (!flag * 255) << 24;             
     unsigned int rb0, rb1, rb2, rb3, g0, g1, g2, g3;                
     uint32_t colors[4], pixels;                                     
                                                                     
     c0 = AV_RL16(s);                                                
     c1 = AV_RL16(s+2);                                              
                                                                     
     rb0  = (c0<<3 | c0<<8) & 0xf800f8;                              
     rb1  = (c1<<3 | c1<<8) & 0xf800f8;                              
     rb0 +=        (rb0>>5) & 0x070007;                              
     rb1 +=        (rb1>>5) & 0x070007;                              
     g0   =        (c0 <<5) & 0x00fc00;                              
     g1   =        (c1 <<5) & 0x00fc00;                              
     g0  +=        (g0 >>6) & 0x000300;                              
     g1  +=        (g1 >>6) & 0x000300;                              
                                                                     
     colors[0] = rb0 + g0 + a;                                       
     colors[1] = rb1 + g1 + a;                                       
                                                                     
     if (c0 > c1 || flag) {                                          
         rb2 = (((2*rb0+rb1) * 21) >> 6) & 0xff00ff;                 
         rb3 = (((2*rb1+rb0) * 21) >> 6) & 0xff00ff;                 
         g2  = (((2*g0 +g1 ) * 21) >> 6) & 0x00ff00;                 
         g3  = (((2*g1 +g0 ) * 21) >> 6) & 0x00ff00;                 
         colors[3] = rb3 + g3 + a;                                   
     } else {                                                        
         rb2 = ((rb0+rb1) >> 1) & 0xff00ff;                          
         g2  = ((g0 +g1 ) >> 1) & 0x00ff00;                          
         colors[3] = 0;                                              
     }                                                               
                                                                     
     colors[2] = rb2 + g2 + a;                                       
                                                                     
     pixels = AV_RL32(s+4);                                          
     for (y=0; y<4; y++) {                                           
         for (x=0; x<4; x++) {                                       
             a        = (alpha & 0x0f) << 28;                        
             a       += a >> 4;                                      
             d[x]     = a + colors[pixels&3];                        
             pixels >>= 2;                                           
             alpha  >>= 4;                                           
         }                                                           
         d += qstride;                                               
     }                                                               
 }

typedef unsigned char etc1_byte;
typedef int etc1_bool;
typedef unsigned int etc1_uint32;

typedef struct EtcCompressed {
    etc1_uint32 high;
    etc1_uint32 low;
    etc1_uint32 score; // Lower is more accurate
} etc_compressed;

/* From http://www.khronos.org/registry/gles/extensions/OES/OES_compressed_ETC1_RGB8_texture.txt

 The number of bits that represent a 4x4 texel block is 64 bits if
 <internalformat> is given by ETC1_RGB8_OES.

 The data for a block is a number of bytes,

 {q0, q1, q2, q3, q4, q5, q6, q7}

 where byte q0 is located at the lowest memory address and q7 at
 the highest. The 64 bits specifying the block is then represented
 by the following 64 bit integer:

 int64bit = 256*(256*(256*(256*(256*(256*(256*q0+q1)+q2)+q3)+q4)+q5)+q6)+q7;

 ETC1_RGB8_OES:

 a) bit layout in bits 63 through 32 if diffbit = 0

 63 62 61 60 59 58 57 56 55 54 53 52 51 50 49 48
 -----------------------------------------------
 | base col1 | base col2 | base col1 | base col2 |
 | R1 (4bits)| R2 (4bits)| G1 (4bits)| G2 (4bits)|
 -----------------------------------------------

 47 46 45 44 43 42 41 40 39 38 37 36 35 34  33  32
 ---------------------------------------------------
 | base col1 | base col2 | table  | table  |diff|flip|
 | B1 (4bits)| B2 (4bits)| cw 1   | cw 2   |bit |bit |
 ---------------------------------------------------


 b) bit layout in bits 63 through 32 if diffbit = 1

 63 62 61 60 59 58 57 56 55 54 53 52 51 50 49 48
 -----------------------------------------------
 | base col1    | dcol 2 | base col1    | dcol 2 |
 | R1' (5 bits) | dR2    | G1' (5 bits) | dG2    |
 -----------------------------------------------

 47 46 45 44 43 42 41 40 39 38 37 36 35 34  33  32
 ---------------------------------------------------
 | base col 1   | dcol 2 | table  | table  |diff|flip|
 | B1' (5 bits) | dB2    | cw 1   | cw 2   |bit |bit |
 ---------------------------------------------------


 c) bit layout in bits 31 through 0 (in both cases)

 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16
 -----------------------------------------------
 |       most significant pixel index bits       |
 | p| o| n| m| l| k| j| i| h| g| f| e| d| c| b| a|
 -----------------------------------------------

 15 14 13 12 11 10  9  8  7  6  5  4  3   2   1  0
 --------------------------------------------------
 |         least significant pixel index bits       |
 | p| o| n| m| l| k| j| i| h| g| f| e| d| c | b | a |
 --------------------------------------------------


 Add table 3.17.2: Intensity modifier sets for ETC1 compressed textures:

 table codeword                modifier table
 ------------------        ----------------------
 0                     -8  -2  2   8
 1                    -17  -5  5  17
 2                    -29  -9  9  29
 3                    -42 -13 13  42
 4                    -60 -18 18  60
 5                    -80 -24 24  80
 6                   -106 -33 33 106
 7                   -183 -47 47 183


 Add table 3.17.3 Mapping from pixel index values to modifier values for
 ETC1 compressed textures:

 pixel index value
 ---------------
 msb     lsb           resulting modifier value
 -----   -----          -------------------------
 1       1            -b (large negative value)
 1       0            -a (small negative value)
 0       0             a (small positive value)
 0       1             b (large positive value)


 */

static const int kModifierTable[] = {
/* 0 */2, 8, -2, -8,
/* 1 */5, 17, -5, -17,
/* 2 */9, 29, -9, -29,
/* 3 */13, 42, -13, -42,
/* 4 */18, 60, -18, -60,
/* 5 */24, 80, -24, -80,
/* 6 */33, 106, -33, -106,
/* 7 */47, 183, -47, -183 };

static const int kLookup[8] = { 0, 1, 2, 3, -4, -3, -2, -1 };

static inline etc1_byte etc1_clamp(int x) {
    return (etc1_byte) (x >= 0 ? (x < 255 ? x : 255) : 0);
}

static
inline int convert4To8(int b) {
    int c = b & 0xf;
    return (c << 4) | c;
}

static
inline int convert5To8(int b) {
    int c = b & 0x1f;
    return (c << 3) | (c >> 2);
}

static
inline int convert6To8(int b) {
    int c = b & 0x3f;
    return (c << 2) | (c >> 4);
}

static
inline int divideBy255(int d) {
    return (d + 128 + (d >> 8)) >> 8;
}

static
inline int convert8To4(int b) {
    int c = b & 0xff;
    return divideBy255(c * 15);
}

static
inline int convert8To5(int b) {
    int c = b & 0xff;
    return divideBy255(c * 31);
}

static
inline int convertDiff(int base, int diff) {
    return convert5To8((0x1f & base) + kLookup[0x7 & diff]);
}

static
inline void take_best(etc_compressed* a, const etc_compressed* b) {
    if (a->score > b->score) {
        *a = *b;
    }
}

static void writeBigEndian(etc1_byte* pOut, etc1_uint32 d) {
    pOut[0] = (etc1_byte)(d >> 24);
    pOut[1] = (etc1_byte)(d >> 16);
    pOut[2] = (etc1_byte)(d >> 8);
    pOut[3] = (etc1_byte) d;
}

static bool inRange4bitSigned(int color) {
    return color >= -4 && color <= 3;
}

static
inline int square(int x) {
    return x * x;
}

static
void etc_average_colors_subblock(const etc1_byte* pIn, etc1_uint32 inMask, etc1_byte* pColors, bool flipped, bool second) {
    int r = 0;
    int g = 0;
    int b = 0;

    if (flipped) {
        int by = 0;
        if (second) {
            by = 2;
        }
        for (int y = 0; y < 2; y++) {
            int yy = by + y;
            for (int x = 0; x < 4; x++) {
                int i = x + 4 * yy;
                if (inMask & (1 << i)) {
                    const etc1_byte* p = pIn + i * 3;
                    r += *(p++);
                    g += *(p++);
                    b += *(p++);
                }
            }
        }
    } else {
        int bx = 0;
        if (second) {
            bx = 2;
        }
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 2; x++) {
                int xx = bx + x;
                int i = xx + 4 * y;
                if (inMask & (1 << i)) {
                    const etc1_byte* p = pIn + i * 3;
                    r += *(p++);
                    g += *(p++);
                    b += *(p++);
                }
            }
        }
    }
    pColors[0] = (etc1_byte)((r + 4) >> 3);
    pColors[1] = (etc1_byte)((g + 4) >> 3);
    pColors[2] = (etc1_byte)((b + 4) >> 3);
}

static
void etc_encodeBaseColors(etc1_byte* pBaseColors, const etc1_byte* pColors, etc_compressed* pCompressed) {
    int r1, g1, b1, r2, g2, b2; // 8 bit base colors for sub-blocks
    bool differential;
    {
        int r51 = convert8To5(pColors[0]);
        int g51 = convert8To5(pColors[1]);
        int b51 = convert8To5(pColors[2]);
        int r52 = convert8To5(pColors[3]);
        int g52 = convert8To5(pColors[4]);
        int b52 = convert8To5(pColors[5]);

        r1 = convert5To8(r51);
        g1 = convert5To8(g51);
        b1 = convert5To8(b51);

        int dr = r52 - r51;
        int dg = g52 - g51;
        int db = b52 - b51;

        differential = inRange4bitSigned(dr) && inRange4bitSigned(dg)
                && inRange4bitSigned(db);
        if (differential) {
            r2 = convert5To8(r51 + dr);
            g2 = convert5To8(g51 + dg);
            b2 = convert5To8(b51 + db);
            pCompressed->high |= (r51 << 27) | ((7 & dr) << 24) | (g51 << 19)
                    | ((7 & dg) << 16) | (b51 << 11) | ((7 & db) << 8) | 2;
        }
    }

    if (!differential) {
        int r41 = convert8To4(pColors[0]);
        int g41 = convert8To4(pColors[1]);
        int b41 = convert8To4(pColors[2]);
        int r42 = convert8To4(pColors[3]);
        int g42 = convert8To4(pColors[4]);
        int b42 = convert8To4(pColors[5]);
        r1 = convert4To8(r41);
        g1 = convert4To8(g41);
        b1 = convert4To8(b41);
        r2 = convert4To8(r42);
        g2 = convert4To8(g42);
        b2 = convert4To8(b42);
        pCompressed->high |= (r41 << 28) | (r42 << 24) | (g41 << 20) | (g42
                << 16) | (b41 << 12) | (b42 << 8);
    }
    pBaseColors[0] = r1;
    pBaseColors[1] = g1;
    pBaseColors[2] = b1;
    pBaseColors[3] = r2;
    pBaseColors[4] = g2;
    pBaseColors[5] = b2;
}

static etc1_uint32 chooseModifier(const etc1_byte* pBaseColors,
        const etc1_byte* pIn, etc1_uint32 *pLow, int bitIndex,
        const int* pModifierTable) {
    etc1_uint32 bestScore = ~0;
    int bestIndex = 0;
    int pixelR = pIn[0];
    int pixelG = pIn[1];
    int pixelB = pIn[2];
    int r = pBaseColors[0];
    int g = pBaseColors[1];
    int b = pBaseColors[2];
    for (int i = 0; i < 4; i++) {
        int modifier = pModifierTable[i];
        int decodedG = etc1_clamp(g + modifier);
        etc1_uint32 score = (etc1_uint32) (6 * square(decodedG - pixelG));
        if (score >= bestScore) {
            continue;
        }
        int decodedR = etc1_clamp(r + modifier);
        score += (etc1_uint32) (3 * square(decodedR - pixelR));
        if (score >= bestScore) {
            continue;
        }
        int decodedB = etc1_clamp(b + modifier);
        score += (etc1_uint32) square(decodedB - pixelB);
        if (score < bestScore) {
            bestScore = score;
            bestIndex = i;
        }
    }
    etc1_uint32 lowMask = (((bestIndex >> 1) << 16) | (bestIndex & 1))
            << bitIndex;
    *pLow |= lowMask;
    return bestScore;
}

static
void etc_encode_subblock_helper(const etc1_byte* pIn, etc1_uint32 inMask, etc_compressed* pCompressed, bool flipped, bool second, const etc1_byte* pBaseColors, const int* pModifierTable) {
    int score = pCompressed->score;
    if (flipped) {
        int by = 0;
        if (second) {
            by = 2;
        }
        for (int y = 0; y < 2; y++) {
            int yy = by + y;
            for (int x = 0; x < 4; x++) {
                int i = x + 4 * yy;
                if (inMask & (1 << i)) {
                    score += chooseModifier(pBaseColors, pIn + i * 3,
                            &pCompressed->low, yy + x * 4, pModifierTable);
                }
            }
        }
    } else {
        int bx = 0;
        if (second) {
            bx = 2;
        }
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 2; x++) {
                int xx = bx + x;
                int i = xx + 4 * y;
                if (inMask & (1 << i)) {
                    score += chooseModifier(pBaseColors, pIn + i * 3,
                            &pCompressed->low, y + xx * 4, pModifierTable);
                }
            }
        }
    }
    pCompressed->score = score;
}

static
void etc_encode_block_helper(const etc1_byte* pIn, etc1_uint32 inMask, const etc1_byte* pColors, etc_compressed* pCompressed, bool flipped) {
    pCompressed->score = ~0;
    pCompressed->high = (flipped ? 1 : 0);
    pCompressed->low = 0;

    etc1_byte pBaseColors[6];

    etc_encodeBaseColors(pBaseColors, pColors, pCompressed);

    int originalHigh = pCompressed->high;

    const int* pModifierTable = kModifierTable;
    for (int i = 0; i < 8; i++, pModifierTable += 4) {
        etc_compressed temp;
        temp.score = 0;
        temp.high = originalHigh | (i << 5);
        temp.low = 0;
        etc_encode_subblock_helper(pIn, inMask, &temp, flipped, false,
                pBaseColors, pModifierTable);
        take_best(pCompressed, &temp);
    }
    pModifierTable = kModifierTable;
    etc_compressed firstHalf = *pCompressed;
    for (int i = 0; i < 8; i++, pModifierTable += 4) {
        etc_compressed temp;
        temp.score = firstHalf.score;
        temp.high = firstHalf.high | (i << 2);
        temp.low = firstHalf.low;
        etc_encode_subblock_helper(pIn, inMask, &temp, flipped, true,
                pBaseColors + 3, pModifierTable);
        if (i == 0) {
            *pCompressed = temp;
        } else {
            take_best(pCompressed, &temp);
        }
    }
} 
 
// 4 x 4 x 3 x 8  bit + 16 bit in -> 8 * 8 bit out
// Input is a 4 x 4 square of 3-byte pixels in form R, G, B
// inmask is a 16-bit mask where bit (1 << (x + y * 4)) tells whether the corresponding (x,y)
// pixel is valid or not. Invalid pixel color values are ignored when compressing.
// Output is an ETC1 compressed version of the data.
static
void etc1_encode_block(const etc1_byte* pIn, etc1_uint32 inMask, etc1_byte* pOut) {
    etc1_byte colors[6];
    etc1_byte flippedColors[6];
    etc_average_colors_subblock(pIn, inMask, colors, false, false);
    etc_average_colors_subblock(pIn, inMask, colors + 3, false, true);
    etc_average_colors_subblock(pIn, inMask, flippedColors, true, false);
    etc_average_colors_subblock(pIn, inMask, flippedColors + 3, true, true);

    etc_compressed a, b;
    etc_encode_block_helper(pIn, inMask, colors, &a, false);
    etc_encode_block_helper(pIn, inMask, flippedColors, &b, true);
    take_best(&a, &b);
    
    //rsDebug("a.high",a.high);
    //rsDebug("a.low",a.low);
    //rsDebug("a.score",a.score);
    
    writeBigEndian(pOut, a.high);
    writeBigEndian(pOut + 4, a.low);
}

uchar * pInA; // uchar3
uint32_t height;
uint32_t width;
uint32_t pixelSize;
bool containMipmaps;

static etc1_uint32 pullBlockAndMask_from_Raster(uint32_t pixelSize, uint32_t bn, const etc1_byte* pIn,  uint32_t height, uint32_t width, etc1_byte* block, bool containMipmaps) {
    static const unsigned short kYMask[] = { 0x0, 0xf, 0xff, 0xfff, 0xffff };
    static const unsigned short kXMask[] = { 0x0, 0x1111, 0x3333, 0x7777,    
            0xffff };
    
    etc1_uint32 mask = 0;
    
    uint32_t bnMP = bn;
	uint32_t widthMP = width ;
	uint32_t heightMP = height ;
	const etc1_byte* pInMP = pIn;
    
    if(containMipmaps) {
    	// mimaplevel to compress : recursive    	
    	while( bnMP > widthMP * heightMP / 16) {
    		// mimaplevel to compress : recursive
    		bnMP = bnMP - (widthMP * heightMP / 16);
    		pInMP = pInMP + widthMP * heightMP * 2;
    		widthMP = widthMP / 2;
    		heightMP = heightMP / 2;    		
    	}     	
    }
                            
    etc1_uint32 encodedWidth = (widthMP + 3) & ~3;
    etc1_uint32 encodedHeight = (widthMP + 3) & ~3;
    
    //rsDebug("encodedWidth", encodedWidth);
    //rsDebug("encodedHeight", encodedHeight);
    
    int by = bnMP / (encodedWidth / 4);
    int bx = bnMP - (by * (encodedWidth / 4));
    
    //rsDebug("bn", bn);
    //rsDebug("by", by);
    //rsDebug("bx", bx);
    
    int yEnd=4;
	if(by == (encodedHeight/4)) {
		yEnd = encodedHeight - heightMP;
	}
	int ymask = kYMask[yEnd];
	
	int xEnd=4;	
	if(bx == (encodedWidth/4)) {
		xEnd = encodedWidth - widthMP;
	}
	mask = ymask & kXMask[xEnd];
    
    int stride = pixelSize * widthMP;  
    
    int x = bx * 4;
	int y = by * 4;
	
	for (int cy = 0; cy < yEnd; cy++) {
		etc1_byte* q = block + (cy * 4) * 3;
		const etc1_byte* p = pInMP + pixelSize * x + stride * (y + cy);
		for (int cx = 0; cx < xEnd; cx++) {
			if(pixelSize == 2) {
				// RGB 565
				int pixel = (p[1] << 8) | p[0];
	            *q++ = convert5To8(pixel >> 11);
	            *q++ = convert6To8(pixel >> 5);
	            *q++ = convert5To8(pixel);
	            p += pixelSize;
			} else {
				// ARGB 8888
				// alpha p[3];
	            *q++ = p[0];
	            *q++ = p[1];
	            *q++ = p[2];
	            p += pixelSize;
			}
		}
	}

    return mask;
}

static etc1_uint32 pullBlockAndMask_from_DXT3(uint32_t bn, const etc1_byte* pIn,  uint32_t height, uint32_t width, etc1_byte* block) {
	static const int pixelSize = 1;
	int stride = pixelSize * width;  
	//ff_decode_dxt3(pIn,block,width,height,stride);
	
	return 0xffff;
}         

// processing of one ETC1 block
ushort4 __attribute__((kernel)) root(uint32_t x)  {
		//rsDebug("===========root==================",x);

		etc1_byte pOut [8];
		etc1_byte block [48];
		
		//  R, G, B. Byte (3 * (x + 4 * y) is the R value of pixel (x, y)
		
		//rsDebug("pInA", pInA);
		etc1_uint32 amask = pullBlockAndMask_from_Raster(pixelSize, x, pInA, height, width, block, containMipmaps);
		//rsDebug("mask",amask);
		//for (int i = 0; i < 48; i++) {
		//	rsDebug("pixel",block[i]);
		//}
		
		//rsDebug("etc1_encode_block call",0);
		etc1_encode_block (block, amask, pOut);
		
		//rsDebug("pOut[0]",pOut[0]);
		//rsDebug("pOut[1]",pOut[1]);
		//rsDebug("pOut[2]",pOut[2]);
		//rsDebug("pOut[3]",pOut[3]);
		//rsDebug("pOut[4]",pOut[4]);
		//rsDebug("pOut[5]",pOut[5]);
		//rsDebug("pOut[6]",pOut[6]);
		//rsDebug("pOut[7]",pOut[7]);
		
		ushort4 out;		
		out.x = pOut[0] | pOut[1] << 8;
		out.y = pOut[2] | pOut[3] << 8;
		out.z = pOut[4] | pOut[5] << 8;
		out.w = pOut[6] | pOut[7] << 8;
		
		//rsDebug("out",out);
		
	 	return out;
}
