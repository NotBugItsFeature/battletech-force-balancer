/*
Copyright (c) 2008, George Blouin Jr. (skyhigh@solaris7.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of
conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, this list
of conditions and the following disclaimer in the documentation and/or other materials
provided with the distribution.
    * Neither the name of George Blouin Jr nor the names of contributors may be
used to endorse or promote products derived from this software without specific prior
written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package BFB;

import BFB.IO.PrintSheet;
import BFB.Common.CommonTools;
import BFB.Common.Constants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import org.w3c.dom.Node;

/**
 *
 * @author gblouin
 */
public class Force extends AbstractTableModel {
    public Vector Units = new Vector();
    public String ForceName = "",
                  LogoPath = "";
    public float TotalBaseBV = 0.0f,
                 TotalModifier = 0.0f,
                 TotalTonnage = 0.0f,
                 TotalC3BV = 0.0f,
                 TotalSkillBV = 0.0f,
                 TotalModifierBV = 0.0f,
                 TotalAdjustedBV = 0.0f,
                 TotalForceBV = 0.0f,
                 UnevenForceMod = 0.0f,
                 TotalForceBVAdjusted = 0.0f;
    public int  NumC3 = 0,
                OpForSize = 0;
    public boolean isDirty = false,
                    useUnevenForceMod = true;

    public Force( ){

    }

    public Force(Node ForceNode) throws Exception {
        Load( ForceNode );
    }
    
    public void Load( Node ForceNode ) throws Exception {
        this.ForceName = ForceNode.getAttributes().getNamedItem("name").getTextContent().trim();
        this.LogoPath = ForceNode.getAttributes().getNamedItem("logo").getTextContent().trim();
        for (int i=0; i < ForceNode.getChildNodes().getLength(); i++) {
            Node n = ForceNode.getChildNodes().item(i);
            if (n.getNodeName().equals("unit")) {
                try {
                    Units.add(new Unit(n));
                } catch (Exception e) {
                    throw e;
                }
            }
        }
        RefreshBV();
    }

    public void RefreshBV() {
        Unit u;
        NumC3 = 0;
        TotalBaseBV = 0.0f;
        TotalModifier = 0.0f;
        TotalTonnage = 0.0f;
        TotalC3BV = 0.0f;
        TotalSkillBV = 0.0f;
        TotalModifierBV = 0.0f;
        TotalAdjustedBV = 0.0f;
        TotalForceBV = 0.0f;
        for( int i = 0; i < Units.size(); i++ ) {
            u = (Unit) Units.get( i );
            TotalBaseBV += u.BaseBV;
            TotalModifier += u.MiscMod;
            TotalTonnage += u.Tonnage;
            TotalSkillBV += u.SkillsBV;
            TotalModifierBV += u.ModifierBV;
            TotalAdjustedBV += u.TotalBV;
            if (u.UsingC3) {
                NumC3++;
            }
        }
        
        if (NumC3 > 0){
            TotalC3BV += (TotalAdjustedBV * 0.05) * NumC3;
        }
        
        TotalForceBV += TotalAdjustedBV + TotalC3BV;
        UnevenForceMod = CommonTools.GetForceSizeMultiplier(OpForSize, Units.size());
        if (Units.size() > OpForSize) {
            TotalForceBVAdjusted = TotalForceBV * UnevenForceMod;
        } else {
            TotalForceBVAdjusted = TotalForceBV;
        }

        fireTableDataChanged();
    }
    
    public void AddUnit( Unit u ) {
        //if( ! Units.contains( u ) ) {
            u.Refresh();
            Units.add( u );
            RefreshBV();
        //}
        isDirty = true;
    }
    
    public void RemoveUnit( Unit u ){
        Units.remove(u);
        RefreshBV();
        isDirty = true;
    }

    public void SerializeXML(BufferedWriter file) throws IOException {
        String tab = "    ";
        Unit u = null;

        file.write( tab + "<force name=\"" + this.ForceName + "\" logo=\"" + this.LogoPath + "\">" );
        file.newLine();

        for (int i = 0; i < this.Units.size(); i++) {
            u = (Unit) Units.get(i);
            file.write(CommonTools.tab + CommonTools.tab + "<unit>");
            file.newLine();

            u.SerializeXML(file);

            file.write(CommonTools.tab + CommonTools.tab + "</unit>");
            file.newLine();
        }

        file.write( tab + "</force>" );
        file.newLine();
        isDirty = false;
    }

    public void SerializeMUL(BufferedWriter file) throws IOException {
        file.write("<unit>");
        file.newLine();

        for (int i = 0; i < this.Units.size(); i++) {
            Unit u = (Unit) Units.get(i);
            u.SerializeMUL(file);
        }

        file.write("</unit>");
        file.newLine();
    }

    public String SerializeClipboard() {
        String data = "";

        data += this.ForceName + Constants.NL;
        for (int s=0; s < 80; s++ ) { data += "-"; }
        data += Constants.NL;
        data += "Unit" + Constants.Tab + Constants.Tab +
                "Tons" + Constants.Tab +
                "BV" + Constants.Tab +
                "Mechwarrior" + Constants.Tab + Constants.Tab +
                "G/P" + Constants.Tab +
                "Adj BV" + Constants.NL;

        for ( int i=0; i < this.Units.size(); i++ ) {
            data += ((Unit) Units.get(i)).SerializeClipboard() + Constants.NL;
        }

        return data;
    }

    public void RenderPrint(PrintSheet p) {
        p.setFont(CommonTools.SectionHeaderFont);
        p.WriteStr(ForceName, 0);
        p.NewLine();
        p.NewLine();
        p.setFont(CommonTools.ItalicFont);
        
        //Output column Headers
        p.WriteStr("Units", 120);
        p.WriteStr("Mechwarrior", 140);
        p.WriteStr("Type", 60);
        p.WriteStr("Tonnage", 50);
        p.WriteStr("Base BV", 50);
        p.WriteStr("G/P", 30);
        //p.WriteStr("Skills BV", 50);
        p.WriteStr("Modifier", 40);
        //p.WriteStr("Pre-C3 BV", 50);
        p.WriteStr("Use C3", 50);
        //p.WriteStr("C3 BV", 30);
        p.WriteStr("Total BV", 50);
        p.WriteStr("Force BV", 0);
        p.NewLine();
        p.setFont(CommonTools.PlainFont);
        
        for (int i=0; i < Units.size(); i++) {
            Unit u = (Unit) Units.get(i);
            u.RenderPrint(p);
        }

        p.WriteLine();

        //Outut Totals
        p.setFont(CommonTools.ItalicFont);
        p.WriteStr(Units.size() + " Units", 120);
        p.WriteStr("", 140);
        p.WriteStr("", 60);
        p.WriteStr(String.format("%1$,.2f", TotalTonnage), 50);
        p.WriteStr(String.format("%1$,.0f", TotalBaseBV), 50);
        p.WriteStr("", 30);
        //p.WriteStr(String.format("%1$,.0f", TotalSkillBV), 50);
        p.WriteStr("", 40);
        //p.WriteStr(String.format("%1$,.0f", TotalAdjustedBV ), 50);
        p.WriteStr("", 50);
        //p.WriteStr(String.format("%1$,.0f", TotalC3BV), 30);
        p.setFont(CommonTools.BoldFont);
        p.WriteStr(String.format("%1$,.0f", TotalForceBV), 50);
        p.WriteStr(String.format("%1$,.0f", TotalForceBVAdjusted), 0);
        p.NewLine();
        p.setFont(CommonTools.PlainFont);
    }

    public void Clear() {
        Units.removeAllElements();
        ForceName = "";
        LogoPath = "";
        TotalBaseBV = 0.0f;
        TotalModifier = 0.0f;
        TotalTonnage = 0.0f;
        TotalC3BV = 0.0f;
        TotalSkillBV = 0.0f;
        TotalModifierBV = 0.0f;
        TotalAdjustedBV = 0.0f;
        TotalForceBV = 0.0f;
        UnevenForceMod = 0.0f;
        TotalForceBVAdjusted = 0.0f;
        NumC3 = 0;
        OpForSize = 0;
        isDirty = false;
        useUnevenForceMod = false;
        RefreshBV();
    }

    public boolean isSaveable() {
        boolean Flag = true;
        RefreshBV();
        if ( ForceName.isEmpty() ) { Flag = false; }
        if ( Units.size() == 0 ) { Flag = false; }
        return Flag;
    }

    public void setupTable( JTable tbl ) {
        tbl.setModel(this);

        //Create a sorting class and apply it to the list
        TableRowSorter Leftsorter = new TableRowSorter<Force>(this);
        List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(3, SortOrder.ASCENDING));
        sortKeys.add(new RowSorter.SortKey(7, SortOrder.ASCENDING));
        Leftsorter.setSortKeys(sortKeys);
        tbl.setRowSorter(Leftsorter);

        tbl.getColumnModel().getColumn(0).setPreferredWidth(150);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(50);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(150);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(20);
        tbl.getColumnModel().getColumn(6).setPreferredWidth(20);
        tbl.getColumnModel().getColumn(7).setPreferredWidth(20);
        tbl.getColumnModel().getColumn(8).setPreferredWidth(30);
    }

    @Override
    public String getColumnName( int col ) {
        switch( col ) {
            case 0:
                return "Unit";
            case 1:
                return "Type";
            case 2:
                return "Mechwarrior";
            case 3:
                return "Tons";
            case 4:
                return "Base BV";
            case 5:
                return "G";
            case 6:
                return "P";
            case 7:
                return "Mod";
            case 8:
                return "C3";
            case 9:
                return "Adj BV";
        }
        return "";
    }
    public int getRowCount() { return Units.size(); }
    public int getColumnCount() { return 10; }
    @Override
    public Class getColumnClass(int c) {
        if (Units.size() > 0) {
            return getClassOf(0, c).getClass();
        } else {
            return String.class;
        }
    }
    public Object getClassOf( int row, int col ) {
        Unit u = (Unit) Units.get( row );
        switch( col ) {
            case 0:
                return u.TypeModel;
            case 1:
                return "";
            case 2:
                return u.Mechwarrior;
            case 3:
                return u.Tonnage;
            case 4:
                return u.BaseBV;
            case 5:
                return u.Gunnery;
            case 6:
                return u.Piloting;
            case 7:
                return u.MiscMod;
            case 8:
                return "";
            case 9:
                return "";
        }
        return "";
    }
    public Object getValueAt( int row, int col ) {
        Unit u = (Unit) Units.get( row );
        switch( col ) {
            case 0:
                return u.TypeModel;
            case 1:
                return Constants.UnitTypes[u.UnitType];
            case 2:
                return u.Mechwarrior;
            case 3:
                return u.Tonnage;
            case 4:
                return u.BaseBV;
            case 5:
                return u.Gunnery;
            case 6:
                return u.Piloting;
            case 7:
                return u.MiscMod;
            case 8:
                if( u.UsingC3 ) {
                    return "Yes";
                } else {
                    return "No";
                }
            case 9:
                return String.format( "%1$,.0f", u.TotalBV );
        }
        return null;
    }
    @Override
    public boolean isCellEditable( int row, int col ) {
        switch( col ) {
            case 2:
                return true;
            case 5:
                return true;
            case 6:
                return true;
            case 7:
                return true;
        }
        return false;
    }
    @Override
    public void setValueAt( Object value, int row, int col ) {
        Unit u = (Unit) Units.get( row );
        switch( col ) {
            case 2:
                u.Mechwarrior = value.toString();
                break;
            case 5:
                u.Gunnery = Integer.parseInt(value.toString());
                break;
            case 6:
                u.Piloting = Integer.parseInt(value.toString());
                break;
            case 7:
                u.MiscMod = Float.parseFloat(value.toString());
                break;
        }
        u.Refresh();
        RefreshBV();
    }
}