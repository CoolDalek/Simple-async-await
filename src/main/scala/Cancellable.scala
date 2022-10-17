trait Cancellable:
  
  def isCancelled: Boolean
  
  def cancel(): Boolean

object Cancellable:
  
  def volatile: Cancellable = new:
    @volatile private var cancelled = false
    
    def isCancelled: Boolean = cancelled
    
    def cancel(): Boolean =
      if(!cancelled)
        cancelled = true
        true
      else false
    end cancel
  end volatile
  
  def plain: Cancellable = new:
    private var cancelled = false

    def isCancelled: Boolean = cancelled

    def cancel(): Boolean =
      if (!cancelled)
        cancelled = true
        true
      else false
    end cancel
  end plain

end Cancellable