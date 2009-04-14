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
import org.w3c.dom.Node;
import ssw.components.Mech;
import ssw.filehandlers.MechListData;
import ssw.filehandlers.XMLReader;

public class Unit {
    public String TypeModel = "",
                  Mechwarrior = "",
                  Filename = "",
                  Configuration = "";
    public float BaseBV = 0.0f,
                 MiscMod = 1.0f,
                 Tonnage = 20.0f,
                 SkillsBV = 0.0f,
                 ModifierBV = 0.0f,
                 C3BV = 0.0f,
                 TotalBV = 0.0f;
    public int Piloting = 5,
               Gunnery = 4,
               UnitType = Constants.BattleMech;
    public boolean UsingC3 = false;
    public Mech m = new Mech();

    public Unit(){
    }

    public Unit( MechListData m ) {
        this.TypeModel = m.getName() + " "  + m.getModel();
        this.Tonnage = m.getTonnage();
        this.BaseBV = m.getBV();
        this.Filename = m.getFilename();
        this.Configuration = m.getConfig();
    }

    public Unit( Node n ) throws Exception {
        for (int i=0; i < n.getChildNodes().getLength(); i++) {
            String nodeName = n.getChildNodes().item(i).getNodeName();
            if (nodeName.equals("model")) {TypeModel = n.getChildNodes().item(i).getTextContent();}
            if (nodeName.equals("config")) {Configuration = n.getChildNodes().item(i).getTextContent();}
            if (nodeName.equals("tonnage")) {Tonnage = Float.parseFloat(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("basebv")) {BaseBV = Float.parseFloat(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("modifier")) {MiscMod = Float.parseFloat(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("piloting")) {Piloting = Integer.parseInt(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("gunnery")) {Gunnery = Integer.parseInt(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("unittype")) {UnitType = Integer.parseInt(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("usingc3")) {UsingC3 = Boolean.parseBoolean(n.getChildNodes().item(i).getTextContent());}
            if (nodeName.equals("mechwarrior")) {Mechwarrior = n.getChildNodes().item(i).getTextContent();}
            if (nodeName.equals("ssw")) {
                Filename = n.getChildNodes().item(i).getTextContent();
                ssw.filehandlers.XMLReader read = new XMLReader();
                try
                {
                    m = read.ReadMech(Filename);
                } catch (Exception e) {
                    //do nothing
                }
            }
        }
        this.Refresh();
    }

    public void Refresh() {
        SkillsBV = 0;
        ModifierBV = 0;
        TotalBV = 0;
        SkillsBV += CommonTools.GetSkillBV(BaseBV, Gunnery, Piloting);
        ModifierBV += CommonTools.GetModifierBV(SkillsBV, MiscMod);
        TotalBV += CommonTools.GetFullAdjustedBV(BaseBV, Gunnery, Piloting, MiscMod);
        if (UsingC3) { C3BV += TotalBV * .05;}
    }

    public void UpdateByMech() {
        TypeModel = m.GetFullName();
        Configuration = m.GetLoadout().GetName();
        BaseBV = m.GetCurrentBV();
        Refresh();
    }

    public String GetSkills(){
        return Gunnery + "/" + Piloting;
    }

    public void RenderPrint(PrintSheet p) {
        p.WriteStr(TypeModel, 120);
        p.WriteStr(Mechwarrior, 140);
        p.WriteStr(Constants.UnitTypes[UnitType], 60);
        p.WriteStr(String.format("%1$,.2f", Tonnage), 50);
        p.WriteStr(String.format("%1$,.0f", BaseBV), 50);
        p.WriteStr(GetSkills(), 30);
        //p.WriteStr(String.format("%1$,.0f", SkillsBV), 50);
        p.WriteStr(String.format("%1$,.2f", MiscMod), 40);
        //p.WriteStr(String.format("%1$,.0f", TotalBV), 50);
        p.WriteStr(Boolean.valueOf(UsingC3).toString(), 50);
        //p.WriteStr(String.format("%1$,.0f", C3BV), 30);
        p.WriteStr(String.format("%1$,.0f", TotalBV), 0);
        p.NewLine();
    }

    public void SerializeXML(BufferedWriter file) throws IOException {
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<model>" + this.TypeModel + "</model>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<config>" + this.Configuration + "</config>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<tonnage>" + this.Tonnage + "</tonnage>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<mechwarrior>" + this.Mechwarrior + "</mechwarrior>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<basebv>" + this.BaseBV + "</basebv>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<modifier>" + this.MiscMod + "</modifier>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<piloting>" + this.Piloting + "</piloting>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<gunnery>" + this.Gunnery + "</gunnery>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<unittype>" + this.UnitType + "</unittype>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<usingc3>" + this.UsingC3 + "</usingc3>");
        file.newLine();
        file.write(CommonTools.tab + CommonTools.tab + CommonTools.tab + "<ssw>" + this.Filename + "</ssw>");
        file.newLine();
    }

    public void LoadMech() {
        try {
            XMLReader reader = new XMLReader();
            this.m = reader.ReadMech(this.Filename);
            if ( ! this.Configuration.isEmpty() ) {
                this.m.SetCurLoadout(this.Configuration.trim());
            }
        } catch (Exception ex) {
            //do nothing
        }
    }

}