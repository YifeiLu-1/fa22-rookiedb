package edu.berkeley.cs186.database.concurrency;

import edu.berkeley.cs186.database.TransactionContext;

/**
 * LockUtil is a declarative layer which simplifies multigranularity lock
 * acquisition for the user (you, in the last task of Part 2). Generally
 * speaking, you should use LockUtil for lock acquisition instead of calling
 * LockContext methods directly.
 */
public class LockUtil {
    /**
     * Ensure that the current transaction can perform actions requiring
     * `requestType` on `lockContext`.
     *
     * `requestType` is guaranteed to be one of: S, X, NL.
     *
     * This method should promote/escalate/acquire as needed, but should only
     * grant the least permissive set of locks needed. We recommend that you
     * think about what to do in each of the following cases:
     * - The current lock type can effectively substitute the requested type
     * - The current lock type is IX and the requested lock is S
     * - The current lock type is an intent lock
     * - None of the above: In this case, consider what values the explicit
     *   lock type can be, and think about how ancestor looks will need to be
     *   acquired or changed.
     *
     * You may find it useful to create a helper method that ensures you have
     * the appropriate locks on all ancestors.
     */
    public static void ensureSufficientLockHeld(LockContext lockContext, LockType requestType) {
        // requestType must be S, X, or NL
        assert (requestType == LockType.S || requestType == LockType.X || requestType == LockType.NL);

        // Do nothing if the transaction or lockContext is null
        TransactionContext transaction = TransactionContext.getTransaction();
        if (transaction == null || lockContext == null) return;

        // You may find these variables useful
        LockContext parentContext = lockContext.parentContext();
        LockType effectiveLockType = lockContext.getEffectiveLockType(transaction);
        LockType explicitLockType = lockContext.getExplicitLockType(transaction);

        // TODO(proj4_part2): implement
        if (LockType.substitutable(effectiveLockType, requestType)) {
            return;
        }
        if (explicitLockType == LockType.IX && requestType == LockType.S) {
            lockContext.promote(transaction, LockType.SIX);
        } else if (explicitLockType.isIntent()) {
            if (explicitLockType.equals(LockType.IS) && requestType.equals(LockType.X)) {
                lockContext.promote(transaction, requestType);
                lockContext.escalate(transaction);
            } else {
                lockContext.escalate(transaction);
            }
        } else  {
            LockType parentRequestType = LockType.parentLock(requestType);
            acquireParentIntentLocks(transaction, parentContext, parentRequestType);
            if (explicitLockType == LockType.NL) {
                lockContext.acquire(transaction, requestType);
            } else {
                lockContext.promote(transaction, requestType);
            }
        }
    }


    // TODO(proj4_part2) add any helper methods you want
    private static void acquireParentIntentLocks(TransactionContext transaction, LockContext lockContext, LockType requestType) {
        if (lockContext == null) {
            return;
        }
        LockContext parentContext = lockContext.parentContext();
        LockType explicitLockType = lockContext.getExplicitLockType(transaction);
        acquireParentIntentLocks(transaction, parentContext, requestType);
        if (LockType.substitutable(explicitLockType, requestType)) {
            return;
        }
        if (explicitLockType == LockType.NL) {
            lockContext.acquire(transaction, requestType);
        } else if (explicitLockType == LockType.S && requestType == LockType.IX) {
            lockContext.promote(transaction, LockType.SIX);
        } else {
            lockContext.promote(transaction, requestType);
        }
    }
}
