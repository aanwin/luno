package com.socketio.luno;
/**
 *
 * @author zjy
 */
final public class Card {
    public enum Color{Red, Blue, Green, Yellow, All};
    public enum Special{Draw, Skip, Reverse, Wildcards, WildDraw};
    int num=-1;
    Color color=null;
    Special spec=null;
    int point=0;
    
    public Card(int num, Color color) {
        this.num=num;
        this.color=color;
        this.point=num;
    }
    public Card(Special spec, Color color){
        this.spec=spec;
        this.color=color;
        if(spec==Special.Wildcards||spec==Special.WildDraw)
            this.point=50;
        else
            this.point=20;
    }
    public String getName(){
        if(num!=-1)
            return color.toString()+Integer.toString(num);
        else
            return color.toString()+spec.toString();
    }
}

