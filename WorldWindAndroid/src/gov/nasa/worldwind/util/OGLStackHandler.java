package gov.nasa.worldwind.util;

import java.util.Stack;

import static android.opengl.GLES20.*;

/**
 * Handles openGL state stack
 * Created by kedzie on 5/9/14.
 */
public class OGLStackHandler {

	public static int GL_POLYGON_BIT=8;
	public static int GL_VIEWPORT_BIT=2048;
	public static int GL_SCISSOR_BIT=524288;

	private static class DepthState {
		boolean depthTestEnabled;
		int []iState = new int[2];
		float []depthClear = new float[1];
		
		DepthState save() {
			depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
			glGetIntegerv(GL_DEPTH_WRITEMASK, iState, 0);
			glGetIntegerv(GL_DEPTH_FUNC, iState, 1);
			glGetFloatv(GL_DEPTH_CLEAR_VALUE, depthClear, 0);
			return this;
		}
		
		void restore() {
			if(depthTestEnabled)
				glEnable(GL_DEPTH_TEST);
			else
				glDisable(GL_DEPTH_TEST);
			glDepthMask(iState[0]>0);
			glDepthFunc(iState[1]);
			glClearDepthf(depthClear[0]);
		}
	}

	private static class ColorState {
		boolean blendEnabled;
		int []iState = new int[5];
		float []fState = new float[4];

		ColorState save() {
			blendEnabled = glIsEnabled(GL_BLEND);
			glGetIntegerv(GL_BLEND_SRC_RGB, iState, 0);
			glGetIntegerv(GL_BLEND_SRC_ALPHA, iState, 1);
			glGetIntegerv(GL_BLEND_DST_RGB, iState, 2);
			glGetIntegerv(GL_BLEND_DST_ALPHA, iState, 3);
			glGetIntegerv(GL_BLEND_EQUATION_RGB, iState, 4);
			glGetIntegerv(GL_BLEND_EQUATION_ALPHA, iState, 5);
			glGetFloatv(GL_BLEND_COLOR, fState, 0);
			return this;
		}

		void restore() {
			if(blendEnabled)
				glEnable(GL_BLEND);
			else
				glDisable(GL_BLEND);
			glBlendFuncSeparate(iState[0], iState[1], iState[2],iState[3]);
			glBlendEquationSeparate(iState[4], iState[5]);
			glBlendColor(fState[0], fState[1], fState[2], fState[3]);
		}
	}

	private static class PolygonState {
		boolean cullEnabled;
		boolean polygonOffsetEnabled;
		int []iState = new int[4];

		PolygonState save() {
			cullEnabled = glIsEnabled(GL_CULL_FACE);
			polygonOffsetEnabled = glIsEnabled(GL_POLYGON_OFFSET_FILL);
			glGetIntegerv(GL_CULL_FACE_MODE, iState, 0);
			glGetIntegerv(GL_FRONT_FACE, iState, 1);
			glGetIntegerv(GL_POLYGON_OFFSET_FACTOR, iState, 2);
			glGetIntegerv(GL_POLYGON_OFFSET_UNITS, iState, 3);
			return this;
		}

		void restore() {
			if(cullEnabled)
				glEnable(GL_CULL_FACE);
			else
				glDisable(GL_CULL_FACE);
			glCullFace(iState[0]);
			glFrontFace(iState[1]);
			if(polygonOffsetEnabled)
				glEnable(GL_POLYGON_OFFSET_FILL);
			else
				glDisable(GL_POLYGON_OFFSET_FILL);
			glPolygonOffset(iState[2], iState[3]);
		}
	}

	private static class ScissorState {
		boolean enabled;
		int[] box;

		ScissorState save() {
			enabled = glIsEnabled(GL_SCISSOR_TEST);
			glGetIntegerv(GL_SCISSOR_BOX, box, 0);
			return this;
		}

		void restore() {
			if(enabled)
				glEnable(GL_SCISSOR_TEST);
			else
				glDisable(GL_SCISSOR_TEST);
			glScissor(box[0], box[1], box[2], box[3]);
		}
	}

	private static class StencilState {
		boolean enabled;
		int[] iState = new int[14];

		StencilState save() {
			enabled = glIsEnabled(GL_STENCIL_TEST);
			glGetIntegerv(GL_STENCIL_FUNC, iState, 0);
			glGetIntegerv(GL_STENCIL_VALUE_MASK, iState, 1);
			glGetIntegerv(GL_STENCIL_REF, iState, 2);
			glGetIntegerv(GL_STENCIL_BACK_FUNC, iState, 3);
			glGetIntegerv(GL_STENCIL_BACK_VALUE_MASK, iState, 4);
			glGetIntegerv(GL_STENCIL_BACK_REF, iState, 5);
			glGetIntegerv(GL_STENCIL_FAIL, iState, 6);
			glGetIntegerv(GL_STENCIL_PASS_DEPTH_FAIL, iState, 7);
			glGetIntegerv(GL_STENCIL_PASS_DEPTH_PASS, iState, 8);
			glGetIntegerv(GL_STENCIL_BACK_FAIL, iState, 9);
			glGetIntegerv(GL_STENCIL_BACK_PASS_DEPTH_FAIL, iState, 10);
			glGetIntegerv(GL_STENCIL_BACK_PASS_DEPTH_PASS, iState, 11);
			glGetIntegerv(GL_STENCIL_WRITEMASK, iState, 12);
			glGetIntegerv(GL_STENCIL_BACK_WRITEMASK, iState, 13);
			return this;
		}

		void restore() {
			if(enabled)
				glEnable(GL_STENCIL_TEST);
			else
				glDisable(GL_STENCIL_TEST);
			glStencilFuncSeparate(GL_FRONT, iState[0], iState[1], iState[2]);
			glStencilFuncSeparate(GL_BACK, iState[3], iState[4], iState[5]);
			glStencilOpSeparate(GL_FRONT, iState[6], iState[7], iState[8]);
			glStencilOpSeparate(GL_BACK, iState[9], iState[10], iState[11]);
			glStencilMaskSeparate(GL_FRONT, iState[12]);
			glStencilMaskSeparate(GL_FRONT, iState[13]);
		}
	}

	private static class ViewportState {
		int[] viewport = new int[4];
		float[] depthRange = new float[2];

		ViewportState save() {
			glGetIntegerv(GL_VIEWPORT, viewport, 0);
			glGetFloatv(GL_DEPTH_RANGE, depthRange, 0);
			return this;
		}

		void restore() {
			glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
			glDepthRangef(depthRange[0], depthRange[1]);
		}
	}

	private Stack<DepthState> depthStateStack = new Stack<DepthState>();
	private Stack<ColorState> colorStateStack = new Stack<ColorState>();
	private Stack<PolygonState> polygonStateStack = new Stack<PolygonState>();
	private Stack<ScissorState> scissorStateStack = new Stack<ScissorState>();
	private Stack<StencilState> stencilStateStack = new Stack<StencilState>();
	private Stack<ViewportState> viewportStateStack = new Stack<ViewportState>();

	private int lastPushedBits;

	public void pushAttrib(int bits) {
		this.lastPushedBits = bits;
		if((bits & GL_DEPTH_BUFFER_BIT) == GL_DEPTH_BUFFER_BIT)
			depthStateStack.push(new DepthState().save());
		if((bits & GL_COLOR_BUFFER_BIT) == GL_COLOR_BUFFER_BIT)
			colorStateStack.push(new ColorState().save());
		if((bits & GL_POLYGON_BIT) == GL_POLYGON_BIT)
			polygonStateStack.push(new PolygonState().save());
		if((bits & GL_STENCIL_BUFFER_BIT) == GL_STENCIL_BUFFER_BIT)
			stencilStateStack.push(new StencilState().save());
		if((bits & GL_SCISSOR_BIT) == GL_SCISSOR_BIT)
			scissorStateStack.push(new ScissorState().save());
		if((bits & GL_VIEWPORT_BIT) == GL_VIEWPORT_BIT)
			viewportStateStack.push(new ViewportState().save());
	}

	public void popAttrib() {
		popAttrib(lastPushedBits);
	}

	public void popAttrib(int bits) {
		lastPushedBits = 0;
		if((bits & GL_DEPTH_BUFFER_BIT) == GL_DEPTH_BUFFER_BIT)
			depthStateStack.pop().restore();
		if((bits & GL_COLOR_BUFFER_BIT) == GL_COLOR_BUFFER_BIT)
			colorStateStack.pop().restore();
		if((bits & GL_POLYGON_BIT) == GL_POLYGON_BIT)
			polygonStateStack.pop().restore();
		if((bits & GL_STENCIL_BUFFER_BIT) == GL_STENCIL_BUFFER_BIT)
			stencilStateStack.pop().restore();
		if((bits & GL_SCISSOR_BIT) == GL_SCISSOR_BIT)
			scissorStateStack.pop().restore();
		if((bits & GL_VIEWPORT_BIT) == GL_VIEWPORT_BIT)
			viewportStateStack.pop().restore();
	}

}
