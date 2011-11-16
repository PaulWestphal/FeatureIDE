/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.figures;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.GridLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.RotatableDecoration;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Color;

import de.ovgu.featureide.fm.ui.editors.FeatureUIHelper;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;

/**
 * represents a legend for the feature model
 * 
 * @author Fabian Benduhn
 */
public class LegendFigure extends Figure implements GUIDefaults {

	/**
	 * Height of each Row (should not be smaller than height of symbols)
	 */
	private static final int ROW_HEIGHT = 15;
	/**
	 * Distance between left border and label in each row (should be larger than
	 * width of biggest symbol)
	 */
	private static final int LABEL_PADDING = 30;
	/**
	 * Specific left-padding for Mandatory and Optional rows
	 */
	private static final int MANDATORY_PADDING = 12;
	/**
	 * Specific left-padding for Grouptype rows
	 */
	private static final int GROUPTYPE_PADDING = 5;
	/**
	 * Additional lift for every row except title (to adjust the space between
	 * title and second row)
	 */
	private static final int LIFT = 10;
	/**
	 * Space between abstract/hidden/false Optional/dead features (needs some
	 * more space for the symbols)
	 */
	private static final int LIFT_2 = 12;

	private static final int SYMBOL_SIZE = ROW_HEIGHT;
	private static final String ALTERNATIVE_TOOLTIP = "Alternative group:\n\nExactly one of the features in this group must be selected.";
	private static final String OR_TOOLTIP = "Or Group:\n\nAt least one of the features in this group must be selected.";
	private static final String OPTIONAL_TOOLTIP = "Optional feature:\n\nThis feature does not have to be selected.";
	private static final String MANDATORY_TOOLTIP = "Mandatory feature:\n\nThis feature must be selected whenever its parent is selected.";
	private static final String ABSTRACT_TOOLTIP = "Abstract feature:\n\nThis feature does not contain any implementation modules,\ni.e no corresponding folder is used.";
	private static final String CONCRETE_TOOLTIP = "Concrete feature:\n\nThis feature contains implementation modules,\ni.e. a corresponding folder is used.";
	private static final String HIDDEN_TOOLTIP = "Hidden feature:\n\nThis feature will not be shown in the configuration editor.\nNon-hidden features should determine when to select the feature automatically.";
	private static final String DEAD_TOOLTIP = "Dead feature:\n\nThis feature cannot be selected in any valid configuration.";
	private static final int ABSTRACT = 0;
	private static final int CONCRETE = 1;
	private static final int HIDDEN = 2;
	private static final int DEAD = 3;
	private static final int AND = 4;
	private static final int OR = 5;
	private static final int ALTERNATIVE = 6;

	private final XYLayout layout = new XYLayout();
	public Point newPos;
	private int width;

	@Override
	public boolean useLocalCoordinates() {
		return true;

	}

	public LegendFigure(Point pos, boolean mandatory, boolean optional,
			boolean or, boolean alternative, boolean and, boolean abstrac,
			boolean concrete, boolean hidden, boolean dead, boolean showHidden) {
		setLocation(pos);
		setLayoutManager(layout);
		setBorder(LEGEND_BORDER);
		setLegendSize(mandatory, optional, or, alternative, and, abstrac,
				concrete, hidden, dead, showHidden);
		FeatureUIHelper.setLegendSize(this.getSize());
		FeatureUIHelper.setLegendFigure(this);
		createRows(mandatory, optional, or, alternative, and, abstrac,
				concrete, hidden, dead, showHidden);
		setForegroundColor(LEGEND_FOREGROUND);
		setBackgroundColor(LEGEND_BACKGROUND);
		this.width = LEGEND_WIDTH;
		this.setOpaque(true);
	}

	/**
	 * @param mandatory
	 * @param optional
	 * @param or
	 * @param alternative
	 * @param and
	 * @return
	 */
	private void setLegendSize(boolean mandatory, boolean optional, boolean or,
			boolean alternative, boolean and, boolean _abstract,
			boolean concrete, boolean hidden, boolean dead, boolean showHidden) {
		int height = ROW_HEIGHT * 2 - 5;
		if (mandatory)
			height = height + ROW_HEIGHT;
		if (optional)
			height = height + ROW_HEIGHT;
		if (or)
			height = height + ROW_HEIGHT;
		if (alternative)
			height = height + ROW_HEIGHT;
		// if (and)
		// height = height + ROW_HEIGHT;
		if (_abstract && concrete) {
			height = height + ROW_HEIGHT;
			height = height + ROW_HEIGHT;
		}
		if (hidden && showHidden)
			height = height + ROW_HEIGHT;
		if (dead)
			height = height + ROW_HEIGHT;

		width = LEGEND_WIDTH;
		if (!mandatory && !alternative && !dead) {
			if (!optional && !concrete && !_abstract) {
				width = 50;
			} else {
				width = 80;
			}
		} else if (dead) {
			width = 150;
		}
		this.setSize(width, height);
	}

	private void createRows(boolean mandatory, boolean optional, boolean or,
			boolean alternative, boolean and, boolean abstrac,
			boolean concrete, boolean hidden, boolean dead, boolean showHidden) {

		createRowTitle();
		int row = 2;
		if (mandatory)
			createRowMandatory(row++);
		if (optional)
			createRowOptional(row++);
		if (or)
			createRowOr(row++);
		if (alternative)
			createRowAlternative(row++);
		// if (and)
		// createRowAnd(row);
		if (abstrac && concrete) {
			createRowAbstract(row++);
			createRowConcrete(row++);
		}
		if (hidden && showHidden)
			createRowHidden(row++);
		if (dead)
			createRowDead(row++);

	}

	private void createRowTitle() {
		Label labelTitle = new Label();
		labelTitle.setForegroundColor(FEATURE_FOREGROUND);
		labelTitle.setFont(DEFAULT_FONT);
		labelTitle.setText("Legend:");
		labelTitle.setLabelAlignment(Label.LEFT);
		layout.setConstraint(labelTitle, new Rectangle(3, 0, width, ROW_HEIGHT));
		add(labelTitle);
	}

	// private void createRowAnd(int row, AND) {
	// createGroupTypeSymbol(row, false, false);
	// Label labelOr = createLabel(row, "And");
	// add(labelOr);
	// labelOr.setForegroundColor(FEATURE_FOREGROUND);
	// }

	private void createRowAlternative(int row) {
		createGroupTypeSymbol(row, ALTERNATIVE);
		Label labelOr = createLabel(row, "Alternative", FEATURE_FOREGROUND,
				ALTERNATIVE_TOOLTIP);

		add(labelOr);
	}

	private void createRowOr(int row) {
		createGroupTypeSymbol(row, OR);
		Label labelOr = createLabel(row, "Or", FEATURE_FOREGROUND, OR_TOOLTIP);
		add(labelOr);
	}

	private void createRowOptional(int row) {
		PolylineConnection p = createConnectionTypeSymbol(row, false);
		add(p);
		Label labelMandatory = createLabel(row, "Optional", FEATURE_FOREGROUND,
				OPTIONAL_TOOLTIP);
		add(labelMandatory);
	}

	private void createRowMandatory(int row) {

		PolylineConnection p = createConnectionTypeSymbol(row, true);
		add(p);
		Label labelMandatory = createLabel(row, "Mandatory",
				FEATURE_FOREGROUND, MANDATORY_TOOLTIP);
		add(labelMandatory);

	}

	private void createRowAbstract(int row) {

		createSymbol(row, ABSTRACT);
		Label labelAbstract = createLabel(row, "Abstract", FEATURE_FOREGROUND,
				ABSTRACT_TOOLTIP);
		add(labelAbstract);

	}

	private void createRowConcrete(int row) {

		createSymbol(row, CONCRETE);
		Label labelConcrete = createLabel(row, "Concrete", FEATURE_FOREGROUND,
				CONCRETE_TOOLTIP);
		add(labelConcrete);

	}

	private void createRowHidden(int row) {

		createSymbol(row, HIDDEN);
		Label labelHidden = createLabel(row, "Hidden", HIDDEN_FOREGROUND,
				HIDDEN_TOOLTIP);
		add(labelHidden);

	}

	private void createRowDead(int row) {

		createSymbol(row, DEAD);
		Label labelDead = createLabel(row, "Dead or false optional",
				FEATURE_FOREGROUND, DEAD_TOOLTIP);
		add(labelDead);

	}

	private Label createLabel(int row, String text, Color foreground,
			String tooltip) {
		Label label = new Label(text);
		label.setLabelAlignment(Label.LEFT);
		layout.setConstraint(label, new Rectangle(LABEL_PADDING, ROW_HEIGHT
				* row - LIFT, width - LABEL_PADDING, ROW_HEIGHT));
		label.setForegroundColor(foreground);
		label.setBackgroundColor(DIAGRAM_BACKGROUND);
		label.setFont(DEFAULT_FONT);
		label.setToolTip(createToolTipContent(tooltip));
		return label;
	}

	/**
	 * @param text
	 * @return
	 */
	private Figure createToolTipContent(String text) {
		Figure toolTipContent = new Figure();
		toolTipContent.setLayoutManager(new GridLayout());
		toolTipContent.add(new Label(text));
		return toolTipContent;
	}

	/**
	 * 
	 * @param row
	 *            the row in which the group type symbol shall appear
	 * @param type
	 *            AND, OR, ALTERNATIVE
	 */
	private void createGroupTypeSymbol(int row, int type) {
		boolean fill = true;
		boolean decoration = false;
		String toolTipText="";
		if (type == AND) {
			
			fill = false;
		} else if (type == OR) {
			toolTipText = OR_TOOLTIP;
			decoration = true;
		} else {
			toolTipText=ALTERNATIVE_TOOLTIP;
		}
		// otherwise type must be ALTERNATIVE and decoration = false;

		Point p1 = new Point(GROUPTYPE_PADDING + SYMBOL_SIZE, ROW_HEIGHT * row
				+ SYMBOL_SIZE - LIFT);
		Point p2 = new Point((GROUPTYPE_PADDING + SYMBOL_SIZE / 2), ROW_HEIGHT
				* row - LIFT);
		Point p3 = new Point(GROUPTYPE_PADDING, ROW_HEIGHT * row + SYMBOL_SIZE
				- LIFT);

		RotatableDecoration sourceDecoration = new LegendRelationDecoration(
				fill, p1);
		PolylineConnection line = new PolylineConnection();
		line.setForegroundColor(CONNECTION_FOREGROUND);

		line.setEndpoints(p2, p3);

		if (decoration)
			line.setSourceDecoration(sourceDecoration);
		PolylineConnection line2 = new PolylineConnection();
		line2.setForegroundColor(CONNECTION_FOREGROUND);

		line2.setEndpoints(p2, p1);
		this.add(line);
		this.add(line2);
		Figure toolTipContent = createToolTipContent(toolTipText);
		line.setToolTip(toolTipContent);
		line2.setToolTip(toolTipContent);
		setForegroundColor(CONNECTION_FOREGROUND);

	}

	private PolylineConnection createConnectionTypeSymbol(int row,
			boolean mandatory) {

		PolylineConnection p = new PolylineConnection();
		p.setForegroundColor(CONNECTION_FOREGROUND);
		p.setSourceDecoration(new CircleDecoration(mandatory));

		Point source = new Point(MANDATORY_PADDING, ROW_HEIGHT * row - LIFT
				+ SYMBOL_SIZE / 2);

		Point target = new Point(MANDATORY_PADDING + SYMBOL_SIZE / 2, row
				* ROW_HEIGHT - LIFT);

		p.setEndpoints(source, target);
		String toolTipText;
		if (mandatory)
			toolTipText = MANDATORY_TOOLTIP;
		else
			toolTipText = OPTIONAL_TOOLTIP;
		p.setToolTip(createToolTipContent(toolTipText));
		return p;
	}

	private void createSymbol(int row, int type) {
		int x1 = (SYMBOL_SIZE / 2 - 2);
		int y1 = (ROW_HEIGHT * row - LIFT_2 / 2);
		int x2 = SYMBOL_SIZE + SYMBOL_SIZE / 2;
		int y2 = (ROW_HEIGHT * row + SYMBOL_SIZE - LIFT_2);
		Point p1 = new Point(x1, y1);

		Figure rect = new RectangleFigure();
		String toolTipText = "";
		switch (type) {

		case (ABSTRACT):
			rect.setBorder(ABSTRACT_BORDER);
			rect.setBackgroundColor(ABSTRACT_BACKGROUND);
			toolTipText = ABSTRACT_TOOLTIP;
			break;
		case (CONCRETE):
			rect.setBorder(CONCRETE_BORDER);
			rect.setBackgroundColor(CONCRETE_BACKGROUND);
			toolTipText = CONCRETE_TOOLTIP;
			break;
		case (HIDDEN):
			rect.setBorder(HIDDEN_BORDER_LEGEND);
			toolTipText = HIDDEN_TOOLTIP;
			break;
		case (DEAD):
			rect.setBorder(DEAD_BORDER);
			rect.setBackgroundColor(DEAD_BACKGROUND);
			toolTipText = DEAD_TOOLTIP;
			break;
		}

		rect.setSize(x2 - x1, y2 - y1);
		rect.setLocation(p1);
		rect.setToolTip(createToolTipContent(toolTipText));
		this.add(rect);
	}
}
