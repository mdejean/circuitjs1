/*
    Copyright (C) Paul Falstad and Iain Sharp

    This file is part of CircuitJS1.

    CircuitJS1 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    CircuitJS1 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CircuitJS1.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.lushprojects.circuitjs1.client;

import com.google.gwt.canvas.dom.client.CanvasGradient;

//import java.awt.*;
//import java.util.StringTokenizer;

class DCMotorElm extends CircuitElm {
    double Kv, Ra, inertia, damping, friction;
    double pos, vel;
    final double RADS_PER_RPM = 2.0*Math.PI/60.0;
    
    public DCMotorElm(int xx, int yy) {
        super(xx, yy);
        setDefaults();
    }
    
    public DCMotorElm(int xa, int ya, int xb, int yb, int f,
                    StringTokenizer st) {
        super(xa, ya, xb, yb, f);
        setDefaults();
        try {
            Kv = Double.parseDouble(st.nextToken());
            Ra = Double.parseDouble(st.nextToken());
            inertia = Double.parseDouble(st.nextToken());
            damping = Double.parseDouble(st.nextToken());
            friction = Double.parseDouble(st.nextToken());
        } catch(Exception e) {
        }
    }
    
    void setDefaults() {
        Kv = 5000; //rpm/V, the traditional unit.
        Ra = 1; //ohms
        inertia = 100e-9; //kg-m^2
        damping = 500e-9; //N-m/(rad/s)
        friction = 500e-6; //N-m
        pos = 0; //rad
        vel = 0; //rad/s
    }
    
    String dump() {
        return super.dump() + " " + Kv + " " + " " + Ra + " " + inertia + " " + damping + " " + friction;
    }
    int getDumpType() {
        return 192;
    }
    
    int radius;
    Point center;
    Point top;
    Point bottom;
    
    void setPoints() {
        super.setPoints();
        radius = 20;
        center = interpPoint(point1, point2, .5);
        top = new Point(center.x, center.y-radius);
        bottom = new Point(center.x, center.y+radius);
        calcLeads(16);
    }
    
    void draw(Graphics g) {
        int box_w = 10;
        int box_h = 4;
        setBbox(point1, point2, 4);
        adjustBbox(center.x-radius, center.y-radius-box_h,
                   center.x+radius, center.y+radius+box_h);
        draw2Leads(g);
        g.setColor(Color.white);
        g.drawRect(center.x-box_w/2, center.y-radius-box_h, box_w, radius*2+box_h*2);
        g.fillOval(center.x-radius, center.y-radius, radius*2, radius*2);
        g.setColor(Color.black);
        int ir = radius - 2;
        g.fillOval(center.x - ir, center.y - ir, ir*2, ir*2);
        g.setColor(Color.white);
        int x = (int)(radius * Math.cos(pos));
        int y = (int)(radius * Math.sin(pos));
        g.drawLine(center.x - x, center.y - y, 
                   center.x + x, center.y + y);
        updateDotCount();
        if (sim.dragElm != this) {
            drawDots(g, point1, top, curcount);
            drawDots(g, bottom, point2, curcount);
        }
        drawPosts(g);
    }
    
    void stamp() {
        sim.stampResistor(nodes[0], nodes[1], Ra);
        sim.stampRightSide(nodes[0]);
        sim.stampRightSide(nodes[1]);
    }
    
    boolean nonLinear() {
        return true;
    }
    
    void reset() {
        super.reset();
        pos = 0;
        vel = 0;
        current = 0;
    }
    
    void startIteration() {
        double T = current / (Kv * RADS_PER_RPM) - damping*vel - friction * Math.signum(vel);
        vel += T/inertia * sim.timeStep;
        if (Math.abs(vel) < friction/inertia * sim.timeStep) {
            vel = 0;
        }
        pos = (pos + sim.timeStep * vel) % (2 * Math.PI);
    }
    
    void doStep() {
        sim.stampCurrentSource(nodes[0], nodes[1], -vel/(Kv * RADS_PER_RPM)/Ra);
    }
    
    void calculateCurrent() {
        current = (volts[0] - volts[1] - vel/(Kv * RADS_PER_RPM)) / Ra;
    }
    
    void getInfo(String arr[]) {
        arr[0] = "motor";
        getBasicInfo(arr);
        arr[3] = "position = " + getUnitText(pos,"rad");
        arr[4] = "velocity = " + getUnitText(vel,"rad/s");
        arr[5] = "kinetic energy = " + getUnitText(0.5*inertia*vel*vel, "J");
    }
    
    public EditInfo getEditInfo(int n) {
        if (n == 0)
            return new EditInfo("Torque constant (rpm/V)", Kv, 0, 0);
        if (n == 1)
            return new EditInfo("Armature resistance (ohm)", Ra, 0, 0);
        if (n == 2)
            return new EditInfo("Mechanical inertia (kg-m^2)", inertia, 0, 0);
        if (n == 3)
            return new EditInfo("Mechanical damping (N-m/rad-s)", damping, 0, 0);
        if (n == 4)
            return new EditInfo("Mechanical friction mu*Fn*R (N-m)", friction, 0, 0);
        return null;
    }
    
    public void setEditValue(int n, EditInfo ei) {
        if (n == 0 && ei.value > 0)
            Kv = ei.value;
        if (n == 1 && ei.value > 0)
            Ra = ei.value;
        if (n == 2 && ei.value > 0)
            inertia = ei.value;
        if (n == 3 && ei.value > 0)
            damping = ei.value;
        if (n == 4 && ei.value > 0)
            friction = ei.value;
    }
}