package org.grails.plugin.queuemail

import org.grails.plugin.queuemail.helpers.QueueHelper


class EmailComparator implements Comparator<ComparableFutureTask> {

	@Override
	public int compare(final ComparableFutureTask lhs, final ComparableFutureTask rhs) {
		
		if(lhs instanceof ComparableFutureTask && rhs instanceof ComparableFutureTask) {
			int returnCode=0
			if (lhs.priority < rhs.priority) {
				returnCode=-1
			} else if (lhs.priority > rhs.priority) {
				returnCode=1
			}
			if (lhs.emailExecutor.defaultComparator) {
				return returnCode
			} else {
				boolean slotsFree = QueueHelper.changeMaxPoolSize(
						lhs.emailExecutor,
						lhs.userId,lhs.maxPoolSize,lhs.minPreserve,lhs.priority,lhs.definedPriority,
						lhs.emailExecutor.getActiveCount(),
						lhs.emailExecutor.getCorePoolSize()
						)
				if (returnCode>0 && slotsFree) {
					returnCode=-1
				}
				return returnCode
			}
		}
	}
}