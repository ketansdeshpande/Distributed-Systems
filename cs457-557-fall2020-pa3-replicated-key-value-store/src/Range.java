/**
	Range as start and end range 
	for the nodes
*/
public class Range{

	int start;

	int end;

	public Range(int start, int end){
		this.start = start;
		this.end = end;
	}

	void setStart(int start){
		this.start = start;
	}
	void setEnd(int end){
		this.end = end;
	}

	int getStart(){
		return this.start;
	}

	int getEnd(){
		return this.end;
	}

	public String toString(){
		return "start :"+this.getStart() + " , end :" +this.getEnd();
	}
}