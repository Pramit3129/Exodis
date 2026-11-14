package redis.datastructure;

import java.util.Arrays;

import static redis.protocol.RespWriter.writeRESPArray;

public class List{
    public int len;
    LLNode head;
    LLNode tail;
    public List(){
        this.head = null;
        this.tail = null;
        this.len = 0;
    }

    public int push(String ele) {
        LLNode newNode = new LLNode(ele);

        if (this.head == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            this.tail.next = newNode;
            this.tail = newNode;
        }

        this.len++;

        return this.len;
    }

    public String[] getEle(int start, int stop){

        if(stop < 0){
            stop = this.len + stop;
            if(stop < 0){
                stop = 0;
            }
        }

        if(start < 0){
            start = this.len + start;
            if(start < 0){
                start = 0;
            }
        }

        if((start > stop) || start >= this.len){
           return new String[]{};
        }

        if(stop >= this.len){
            stop = this.len - 1;
        }

        String[] eles = new String[stop-start+1];
        int idx = 0;
        LLNode temp = this.head;
        while (idx < start){
            temp = temp.next;
            idx++;
        }
        while (idx <= stop){
            String curVal = temp.val;
            temp = temp.next;
            eles[idx++ - start] = curVal;
        }
        // System.out.println(Arrays.toString(eles));
        return eles;
    }

}
