package redis.datastructure;

public class RedisList {
    private int len;
    LLNode head;
    LLNode tail;
    public RedisList(){
        this.head = null;
        this.tail = null;
        this.len = 0;
    }

    synchronized public int lpush(String[] elements){

        for(String element : elements){
            enqueInternal(element);
        }

        notifyAll();

        return this.len;
    }

    synchronized public int rpush(String[] elements){

        for(String element : elements){
            pushInternal(element);
        }

        notifyAll();

        return this.len;
    }

    private void enqueInternal(String ele){

        LLNode newNode = new LLNode(ele);

        if(this.head == null){
            this.head = newNode;
            this.tail = newNode;
        }else{
            newNode.next = this.head;
            this.head = newNode;
        }

        this.len++;
    }

    private void pushInternal(String ele){

        LLNode newNode = new LLNode(ele);

        if(this.head == null){
            this.head = newNode;
            this.tail = newNode;
        }else{
            this.tail.next = newNode;
            this.tail = newNode;
        }

        this.len++;
    }

    synchronized public int push(String ele) {
        pushInternal(ele);

        notifyAll();

        return this.len;
    }


    synchronized public int length(){
        return this.len;
    }

    synchronized public int enque(String ele){
        LLNode newNode = new LLNode(ele);

        if (this.head == null) {
            this.head = newNode;
            this.tail = newNode;
        } else {
            newNode.next = this.head;
            this.head = newNode;
        }
        this.len++;
        return this.len;
    }

    synchronized public String[] getEle(int start, int stop){

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

    synchronized public String Lpop(){

        if(this.head == null){
            return null;
        }

        LLNode temp = this.head;

        this.head = this.head.next;
        temp.next = null;

        this.len--;

        if(this.head == null){
            this.tail = null;
        }

        return temp.val;
    }

    synchronized public boolean isEmpty() {
        return this.len == 0;
    }

    synchronized public String BLPOP(long timeoutMillis) throws InterruptedException {

        long deadline = timeoutMillis == 0
                ? 0
                : System.nanoTime() + timeoutMillis * 1_000_000L;

        while (this.len == 0) {

            if (timeoutMillis == 0) {

                // Wait indefinitely
                wait();

            } else {

                long remainingNanos = deadline - System.nanoTime();

                if (remainingNanos <= 0) {
                    return null;
                }

                long remainingMillis = remainingNanos / 1_000_000L;

                int remainingNano = (int) (remainingNanos % 1_000_000L);

                wait(remainingMillis, remainingNano);
            }
        }

        return Lpop();
    }
}
