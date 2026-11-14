package redis.datastructure;

public class List{
    int len;
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

}
